package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent;

/** Adopted damage parameters for native-triggered pulses or an effect-owned periodic behavior. */
public record EnvironmentDamage(NexusId damageType, double amount, double minimumHealth, int intervalTicks) implements BehaviorComponent {
    public EnvironmentDamage {
        java.util.Objects.requireNonNull(damageType);
        if (!Double.isFinite(amount) || amount < 0 || !Double.isFinite(minimumHealth) || minimumHealth < 0 || intervalTicks < 0)
            throw new IllegalArgumentException("Invalid environment damage parameters");
    }
}
