package io.github.fatalitaya.combatnexus.effect.internal;

import java.util.*;

/** Selects original candidate indices, strongest first, retaining only useful hidden lifetimes. */
public final class EffectLayerSelection {
    public record Candidate(int level, long deadline) {}
    private EffectLayerSelection() {}
    public static List<Integer> select(List<Candidate> candidates, long now) {
        var levels = new TreeMap<Integer, Integer>(Comparator.reverseOrder());
        for (int i = 0; i < candidates.size(); i++) {
            var value = candidates.get(i);
            if (value.deadline() > now)
                levels.merge(value.level(), i, (a, b) -> candidates.get(b).deadline() > candidates.get(a).deadline() ? b : a);
        }
        var retained = new ArrayList<Integer>();
        long visibleUntil = -1;
        for (int index : levels.values()) if (candidates.get(index).deadline() > visibleUntil) {
            retained.add(index); visibleUntil = candidates.get(index).deadline();
        }
        return List.copyOf(retained);
    }
    public static OptionalLong rebase(OptionalLong expiry, long savedTick, long now) {
        return expiry.isPresent() ? OptionalLong.of(Math.addExact(now, Math.max(0, expiry.getAsLong() - savedTick))) : OptionalLong.empty();
    }
}
