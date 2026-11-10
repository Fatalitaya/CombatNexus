package io.github.fatalitaya.combatnexus.combat.api.behavior;

import io.github.fatalitaya.combatnexus.combat.api.action.CombatAction;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Frozen prediction after native mitigation. Remaining health damage excludes reserved shields
 * and absorption, and includes earlier health protection. A strict threshold compares
 * predictedHealth() with maximumHealth * threshold. */
public record DamageGuardContext(UUID target, DamageGuardBehavior.Stage stage,
        DamageRequestKind requestKind, Set<NexusId> sourceTags, Optional<CombatAction> action,
        float health, float maximumHealth, float damage, float remainingHealthDamage,
        ReactionInputs inputs, BehaviorSource behavior) {
    public DamageGuardContext { sourceTags = Set.copyOf(sourceTags); }
    public float predictedHealth() { return health - remainingHealthDamage; }
}
