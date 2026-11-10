package io.github.fatalitaya.combatnexus.api.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import java.util.*;
import java.util.stream.Stream;

/** Platform-independent structural preflight, performed before recursive decoding. */
public final class CodecBudgets {
    private CodecBudgets() {}
    public static <T> Codec<List<T>> boundedList(
            Codec<T> elementCodec,
            int maximum,
            String name
    ) {
        return elementCodec
                .listOf(0, maximum)
                .xmap(List::copyOf, List::copyOf)
                .validate(values -> values.size() <= maximum
                        ? DataResult.success(values)
                        : DataResult.error(() ->
                        name + " exceeds maximum " + maximum));
    }

    public static Codec<String> boundedString(
            int maximumCodePoints,
            String name
    ) {
        return Codec.STRING.validate(value -> {
            int length = value.codePointCount(0, value.length());

            if (length <= maximumCodePoints) {
                return DataResult.success(value);
            }

            return DataResult.error(() ->
                    name + " exceeds " + maximumCodePoints
                            + " Unicode code points"
            );
        });
    }

    public static <A> Codec<A> guardRawStructure(
            Codec<A> codec,
            String name,
            int maximumDepth,
            int maximumNodes
    ) {
        return Codec.of(codec, new Decoder<>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(
                    DynamicOps<T> ops,
                    T input
            ) {
                Optional<String> problem = rawProblem(
                        ops,
                        input,
                        maximumDepth,
                        maximumNodes,
                        RawTraversalObserver.NONE
                );

                if (problem.isPresent()) {
                    return DataResult.error(() ->
                            name + " rejected before decode: "
                                    + problem.get()
                    );
                }

                return codec.decode(ops, input);
            }
        });
    }

    /** Text budget runs after structural preflight and before object decoding. */
    public static <A> Codec<A> guardText(Codec<A> codec, int codePoints, int utf8Bytes) {
        return new Codec<>() {
            public <T> DataResult<Pair<A,T>> decode(DynamicOps<T> ops,T input){
                var problem=textProblem(ops,input,codePoints,utf8Bytes);
                return problem.isPresent()?DataResult.error(problem::get):codec.decode(ops,input);
            }
            public <T> DataResult<T> encode(A input,DynamicOps<T> ops,T prefix){
                return codec.encode(input,ops,prefix).flatMap(value->{var problem=textProblem(ops,value,codePoints,utf8Bytes);return problem.isPresent()?DataResult.error(problem::get):DataResult.success(value);});
            }
        };
    }
    private static <T> Optional<String> textProblem(DynamicOps<T> ops,T input,int points,int bytes){
        var pending=new ArrayDeque<T>();pending.push(input);
        while(!pending.isEmpty()){
            var value=pending.pop();var string=ops.getStringValue(value).result();
            if(string.isPresent()){
                var text=string.get();
                for(int offset=0;offset<text.length();){int cp=text.codePointAt(offset);offset+=Character.charCount(cp);bytes-=cp<=0x7f?1:cp<=0x7ff?2:cp<=0xffff?3:4;if(--points<0||bytes<0)return Optional.of("Equipment text budget exceeded");}
                continue;
            }
            var map=ops.getMap(value).result();
            if(map.isPresent()){try(var entries=map.get().entries()){var iterator=entries.iterator();while(iterator.hasNext()){var pair=iterator.next();pending.push(pair.getFirst());pending.push(pair.getSecond());}}}
            else {var list=ops.getStream(value).result();if(list.isPresent())try(var values=list.get()){values.forEach(pending::push);}}
        }
        return Optional.empty();
    }

    public static <T> Optional<String> rawProblem(
            DynamicOps<T> ops,
            T input,
            int maximumDepth,
            int maximumNodes,
            RawTraversalObserver observer
    ) {
        if (maximumDepth <= 0 || maximumNodes <= 0) {
            return Optional.of("raw_budget_is_not_positive");
        }

        RawTraversal<T> traversal = new RawTraversal<>(
                maximumDepth,
                maximumNodes,
                observer == null ? RawTraversalObserver.NONE : observer
        );
        Optional<String> rootProblem = traversal.discover(input, 1);

        if (rootProblem.isPresent()) {
            return rootProblem;
        }

        while (!traversal.stack.isEmpty()) {
            RawFrame<T> frame = traversal.stack.pop();

            Optional<com.mojang.serialization.MapLike<T>> map =
                    ops.getMap(frame.value()).result();

            if (map.isPresent()) {
                try (Stream<Pair<T, T>> entries = map.get().entries()) {
                    Iterator<Pair<T, T>> iterator = entries.iterator();

                    while (iterator.hasNext()) {
                        Pair<T, T> pair = iterator.next();
                        Optional<String> keyProblem = traversal.discover(
                                pair.getFirst(),
                                frame.depth() + 1
                        );

                        if (keyProblem.isPresent()) {
                            return keyProblem;
                        }

                        Optional<String> valueProblem = traversal.discover(
                                pair.getSecond(),
                                frame.depth() + 1
                        );

                        if (valueProblem.isPresent()) {
                            return valueProblem;
                        }
                    }
                }

                continue;
            }

            Optional<Stream<T>> list =
                    ops.getStream(frame.value()).result();

            if (list.isPresent()) {
                try (Stream<T> values = list.get()) {
                    Iterator<T> iterator = values.iterator();

                    while (iterator.hasNext()) {
                        Optional<String> childProblem = traversal.discover(
                                iterator.next(),
                                frame.depth() + 1
                        );

                        if (childProblem.isPresent()) {
                            return childProblem;
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private record RawFrame<T>(T value, int depth) {
    }

    @FunctionalInterface
    public interface RawTraversalObserver {
        RawTraversalObserver NONE =
                (discoveredNodes, pendingNodes) -> {
                };

        void discovered(int discoveredNodes, int pendingNodes);
    }

    private static final class RawTraversal<T> {
        private final int maximumDepth;
        private final int maximumNodes;
        private final RawTraversalObserver observer;
        private final Deque<RawFrame<T>> stack = new ArrayDeque<>();
        private int discoveredNodes;

        private RawTraversal(
                int maximumDepth,
                int maximumNodes,
                RawTraversalObserver observer
        ) {
            this.maximumDepth = maximumDepth;
            this.maximumNodes = maximumNodes;
            this.observer = observer;
        }

        private Optional<String> discover(T value, int depth) {
            if (depth > maximumDepth) {
                return Optional.of(
                        "raw_depth=" + depth
                                + " maximum=" + maximumDepth
                );
            }

            if (discoveredNodes >= maximumNodes) {
                return Optional.of(
                        "raw_nodes=" + ((long) discoveredNodes + 1L)
                                + " maximum=" + maximumNodes
                );
            }

            discoveredNodes++;
            stack.push(new RawFrame<>(value, depth));
            observer.discovered(discoveredNodes, stack.size());
            return Optional.empty();
        }
    }
}
