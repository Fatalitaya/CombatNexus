package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.combat.damage.util.EntityConditionUtil;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record TargetIsBossCondition() implements DamageRuleCondition {

    public static final MapCodec<TargetIsBossCondition> CODEC =
            MapCodec.unit(new TargetIsBossCondition());

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.TARGET_IS_BOSS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return EntityConditionUtil.isBoss(ctx.targetFacts().entityType());
    }
}
