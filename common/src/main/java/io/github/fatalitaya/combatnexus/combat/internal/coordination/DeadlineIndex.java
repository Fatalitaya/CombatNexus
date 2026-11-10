package io.github.fatalitaya.combatnexus.combat.internal.coordination;

import java.util.*;

/** Cancellable deadlines. Due keys remain scheduled until their owner commits a change. */
public final class DeadlineIndex<K> {
    private final NavigableMap<Long, Set<K>> deadlines = new TreeMap<>();
    private final Map<K, Long> times = new HashMap<>();
    public void schedule(K key, long tick) {
        cancel(key); times.put(key, tick);
        deadlines.computeIfAbsent(tick, ignored -> new LinkedHashSet<>()).add(key);
    }
    public void cancel(K key) {
        Long tick = times.remove(key);
        if (tick == null) return;
        var keys = deadlines.get(tick); keys.remove(key);
        if (keys.isEmpty()) deadlines.remove(tick);
    }
    public List<K> due(long tick) {
        var result = new ArrayList<K>();
        deadlines.headMap(tick, true).values().forEach(result::addAll);
        return result;
    }
    public List<K> due(java.util.function.ToLongFunction<K> clock) {
        var result = new ArrayList<K>();
        deadlines.forEach((deadline, keys) -> keys.forEach(key -> {
            if (deadline <= clock.applyAsLong(key)) result.add(key);
        }));
        return result;
    }
    public void clear() { deadlines.clear(); times.clear(); }
}
