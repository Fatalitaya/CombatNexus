package io.github.fatalitaya.combatnexus.combat.api.input;

import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Serialized at capture; decoding returns a detached value on each read. No live objects survive. */
public final class FrozenFacts {
    private final Map<FactKey<?>, String> values;
    private final Set<NexusId> incomplete;
    public FrozenFacts(Map<FactKey<?>, String> values, Set<NexusId> incomplete) {
        this.values = Map.copyOf(values); this.incomplete = Set.copyOf(incomplete);
    }
    public Map<NexusId, String> encodedValues() {
        return values.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                entry -> entry.getKey().id(), Map.Entry::getValue));
    }
    public Set<NexusId> incomplete() { return incomplete; }
    public <T> Optional<T> find(FactKey<T> key) {
        var json = values.get(key);
        return json == null ? Optional.empty() : Optional.of(key.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow());
    }
}
