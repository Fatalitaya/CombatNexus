package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;

import java.util.List;

public interface DamageRuleProvider {

    default boolean supportsPhase(DamagePhase phase) {
        return true;
    }

    void collect(
            NativeDamageRuleContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    );
}
