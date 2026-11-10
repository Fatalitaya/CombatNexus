package io.github.fatalitaya.combatnexus.combat.projectile.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.CombatSource;

/** Short-lived submission. Only the launch tick permits base-damage modification. */
public record ProjectileChange(UUID requestId, UUID target, CombatSource source, ProjectileDamageTarget projectile,
        UUID action, double damageMultiplier) {
    public ProjectileChange {
        Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source);
        Objects.requireNonNull(projectile); Objects.requireNonNull(action);
        if (!Double.isFinite(damageMultiplier) || damageMultiplier < 0) throw new IllegalArgumentException("Invalid projectile multiplier");
    }
}
