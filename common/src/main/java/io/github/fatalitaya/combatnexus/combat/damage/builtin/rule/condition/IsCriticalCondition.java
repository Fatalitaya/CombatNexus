package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record IsCriticalCondition() implements DamageRuleCondition {

    public static final MapCodec<IsCriticalCondition> CODEC =
            MapCodec.unit(new IsCriticalCondition());

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.IS_CRITICAL;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.isCritical();
    }
}
