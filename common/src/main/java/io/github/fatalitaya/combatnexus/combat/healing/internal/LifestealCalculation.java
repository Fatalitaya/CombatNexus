package io.github.fatalitaya.combatnexus.combat.healing.internal;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementStatus;

/** Confirmed health accounting; the platform owns settlement scope and healing submission. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class LifestealCalculation {
    private LifestealCalculation() {}

    public static boolean eligible(
            DamageSettlementStatus status,
            boolean cancelled,
            DamageRequestKind kind,
            boolean parent,
            boolean proc,
            boolean completed,
            boolean observed) {
        return status == DamageSettlementStatus.APPLIED
                && !cancelled
                && kind == DamageRequestKind.PRIMARY
                && !parent
                && proc
                && completed
                && observed;
    }

    public static double amount(double loss, double applied, double before, double ratio) {
        return Math.min(loss, Math.min(applied, before)) * ratio;
    }
}
