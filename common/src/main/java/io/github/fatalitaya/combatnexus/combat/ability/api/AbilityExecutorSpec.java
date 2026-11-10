package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import java.util.Set;

/** Startup declaration used during candidate definition validation and execution admission. */
public record AbilityExecutorSpec(Set<AbilityTarget.Kind> targets, Set<CombatOperation> permissions, int semanticVersion) {
    public AbilityExecutorSpec {
        targets = Set.copyOf(targets); permissions = Set.copyOf(permissions);
        if (semanticVersion < 1) throw new IllegalArgumentException("Executor semantic identity is required");
        if (targets.isEmpty()) throw new IllegalArgumentException("Executor needs a target contract");
    }
}
