package io.github.fatalitaya.combatnexus.combat.api.entry;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Selects candidates without changing their declaration order or collapsing distinct sources. */
public final class EntrySelectionResolver {
    private EntrySelectionResolver() {}

    public static List<EntryDefinition> resolve(List<EntryDefinition> input) {
        return resolve(input, Function.identity());
    }

    public static <T> List<T> resolve(List<T> input, Function<T, EntryDefinition> definition) {
        if (input == null || input.isEmpty()) return List.of();
        boolean[] selected = new boolean[input.size()];
        Map<NexusId, Integer> unique = new HashMap<>();
        Map<NexusId, Integer> groups = new HashMap<>();
        Map<NexusId, Integer> replacements = new HashMap<>();
        for (int index = 0; index < input.size(); index++) {
            T candidate = input.get(index);
            if (candidate == null) continue;
            var value = definition.apply(candidate);
            switch (value.stacking()) {
                case STACK -> selected[index] = true;
                case UNIQUE_ENTRY -> selected[index] = unique.putIfAbsent(value.id(), index) == null;
                case UNIQUE_GROUP -> selected[index] = groups.putIfAbsent(value.stackingKey(), index) == null;
                case REPLACE -> {
                    Integer previous = replacements.put(value.stackingKey(), index);
                    if (previous != null) selected[previous] = false;
                    selected[index] = true;
                }
            }
        }
        List<T> result = new ArrayList<>();
        for (int index = 0; index < input.size(); index++) {
            if (selected[index]) result.add(input.get(index));
        }
        return List.copyOf(result);
    }
}
