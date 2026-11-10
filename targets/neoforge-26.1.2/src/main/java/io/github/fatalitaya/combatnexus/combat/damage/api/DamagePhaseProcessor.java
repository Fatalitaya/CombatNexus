package io.github.fatalitaya.combatnexus.combat.damage.api;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;

public interface DamagePhaseProcessor {

    int PRIORITY_VANILLA = 0;
    int PRIORITY_MOD_STANDARD = 100;
    int PRIORITY_OVERRIDE = 1000;

    void apply(NativeDamageRuleContext ctx);

    DamagePhase phase();

    default int getPriority() {
        return PRIORITY_MOD_STANDARD;
    }

    default boolean canHandle(NativeDamageContextView ctx) {
        return ctx.isManaged();
    }
}
