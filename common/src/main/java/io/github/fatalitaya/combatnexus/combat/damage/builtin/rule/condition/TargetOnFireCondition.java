package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record TargetOnFireCondition() implements DamageRuleCondition {
    public static final MapCodec<TargetOnFireCondition> CODEC =
            MapCodec.unit(new TargetOnFireCondition());

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.TARGET_ON_FIRE;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.targetFacts() != null && ctx.targetFacts().onFire();
    }
}
