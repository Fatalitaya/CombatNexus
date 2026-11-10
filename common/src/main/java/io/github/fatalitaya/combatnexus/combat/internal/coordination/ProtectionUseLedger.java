package io.github.fatalitaya.combatnexus.combat.internal.coordination;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Installation-local use counts and world-clock deadlines. Reads never advance state. */
public final class ProtectionUseLedger {
    private final Map<Long, Map<NexusId, Integer>> counts = new HashMap<>();
    private final Map<Long, Map<NexusId, Long>> readyAt = new HashMap<>();
    public boolean available(long installation, NexusId type, int maximumUses, long now) {
        var deadline = readyAt.getOrDefault(installation, Map.of()).get(type);
        return deadline != null && now >= deadline
                || counts.getOrDefault(installation, Map.of()).getOrDefault(type, 0) < maximumUses;
    }
    public void consume(long installation, NexusId type, int cooldownTicks, long now) {
        var deadline = readyAt.getOrDefault(installation, Map.of()).get(type);
        var uses = counts.computeIfAbsent(installation, ignored -> new HashMap<>());
        if (deadline != null && now >= deadline) uses.remove(type);
        uses.merge(type, 1, Math::addExact);
        if (cooldownTicks > 0) readyAt.computeIfAbsent(installation, ignored -> new HashMap<>()).put(type, Math.addExact(now, cooldownTicks));
        else if (readyAt.containsKey(installation)) readyAt.get(installation).remove(type);
    }
    public Map<NexusId, Integer> consumed(long installation) { return Map.copyOf(counts.getOrDefault(installation, Map.of())); }
    public Map<NexusId, Long> remaining(long installation, long now) {
        var result = new HashMap<NexusId, Long>();
        readyAt.getOrDefault(installation, Map.of()).forEach((type, deadline) -> result.put(type, Math.max(0, deadline - now)));
        return Map.copyOf(result);
    }
    public void restore(long installation, Map<NexusId, Integer> consumed, Map<NexusId, Long> remaining, long now) {
        if (consumed.size() > 64 || remaining.size() > 64 || !consumed.keySet().containsAll(remaining.keySet())
                || consumed.values().stream().anyMatch(value -> value < 0 || value > 64)
                || remaining.values().stream().anyMatch(value -> value < 0 || value > 1200000))
            throw new IllegalArgumentException("Invalid protection progress");
        var deadlines = new HashMap<NexusId, Long>();
        remaining.forEach((type, ticks) -> deadlines.put(type, Math.addExact(now, ticks)));
        counts.put(installation, new HashMap<>(consumed)); readyAt.put(installation, deadlines);
    }
    public void retain(long installation, Set<NexusId> types) {
        if (counts.containsKey(installation)) counts.get(installation).keySet().retainAll(types);
        if (readyAt.containsKey(installation)) readyAt.get(installation).keySet().retainAll(types);
    }
    public void remove(long installation) { counts.remove(installation); readyAt.remove(installation); }
    public void clear() { counts.clear(); readyAt.clear(); }
}
