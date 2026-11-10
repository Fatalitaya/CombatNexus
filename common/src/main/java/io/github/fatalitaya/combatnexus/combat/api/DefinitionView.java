package io.github.fatalitaya.combatnexus.combat.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** Detached catalog description; never an execution definition or a publication capability. */
public record DefinitionView(long generation, Set<NexusId> effects, Set<NexusId> behaviors,
        Set<NexusId> shields, Set<NexusId> regeneration, Set<NexusId> healingRules) {
    public DefinitionView { effects = Set.copyOf(effects); behaviors = Set.copyOf(behaviors); shields = Set.copyOf(shields);
        regeneration = Set.copyOf(regeneration); healingRules = Set.copyOf(healingRules); }
}
