package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;

/** Narrow internal access; restricted extension views never implement these capabilities. */
public final class DamageInternalContexts {
    private DamageInternalContexts() {}
    public static DamageFrozenFacts facts(NativeDamageContextView context, String usage) {
        if (context instanceof DamageFrozenFacts facts) return facts;
        throw new IllegalArgumentException("DamageModule internal facts required for " + usage);
    }
    public static DamagePhaseAccess phase(NativeDamageContextView context, String usage) {
        if (context instanceof DamagePhaseAccess phase) return phase;
        throw new IllegalArgumentException("DamageModule internal phase required for " + usage);
    }
}
