package io.github.fatalitaya.combatnexus.combat.api.behavior;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame;
/** Request-scoped frozen participants and source classification, never re-sampled at settlement. */
public record ReactionInputs(Map<UUID,CombatInputFrame> participants, Optional<NexusId> damageType, Set<NexusId> damageTags) {
    public static final ReactionInputs EMPTY=new ReactionInputs(Map.of(),Optional.empty(),Set.of());
    public ReactionInputs { participants=Map.copyOf(participants);Objects.requireNonNull(damageType);damageTags=Set.copyOf(damageTags); }
}
