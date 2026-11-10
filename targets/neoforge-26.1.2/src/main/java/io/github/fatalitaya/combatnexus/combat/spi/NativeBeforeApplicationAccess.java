package io.github.fatalitaya.combatnexus.combat.spi;

import java.util.UUID;
import io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan;

/** Trusted damage correlation; ordinary adapters use CombatWorld.beforeApplication. */
public final class NativeBeforeApplicationAccess {
    private NativeBeforeApplicationAccess() {}
    public static boolean available(UUID target) {
        return io.github.fatalitaya.combatnexus.combat.damage.core.settlement.DamageSettlementTracker.acceptsBeforePlan(target);
    }
    public static void record(UUID target, BeforeApplicationPlan.Result result) {
        io.github.fatalitaya.combatnexus.combat.damage.core.settlement.DamageSettlementTracker.recordBeforePlan(target, result);
    }
}
