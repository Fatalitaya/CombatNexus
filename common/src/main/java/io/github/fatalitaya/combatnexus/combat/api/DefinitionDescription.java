package io.github.fatalitaya.combatnexus.combat.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** Read-only adopted definition and its publication identity. */
public record DefinitionDescription(long generation, NexusId id, Kind kind, Map<String,String> parameters) {
    public enum Kind { EFFECT, BEHAVIOR, SHIELD, REGENERATION, HEALING_RULE }
    public DefinitionDescription { Objects.requireNonNull(id);Objects.requireNonNull(kind);parameters=Map.copyOf(parameters); }
}
