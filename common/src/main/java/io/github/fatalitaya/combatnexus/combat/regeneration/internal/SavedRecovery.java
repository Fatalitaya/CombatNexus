package io.github.fatalitaya.combatnexus.combat.regeneration.internal;

import java.util.Objects;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationPlan;
import io.github.fatalitaya.combatnexus.formula.Amounts;

public record SavedRecovery(RegenerationPlan plan, double accrued, int ticks, Long damageAge, long remainingDelay) {
    public SavedRecovery {
        Objects.requireNonNull(plan);
        if (remainingDelay < 0 || remainingDelay > 1 || ticks < 0 || ticks >= 20 || !Double.isFinite(accrued) || accrued < 0 || damageAge != null && damageAge < 0)
            throw new IllegalArgumentException("Invalid saved recovery progress");
    }
}
