package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record HasParentDamageCondition() implements DamageRuleCondition {
    public static final MapCodec<HasParentDamageCondition> CODEC =
            MapCodec.unit(new HasParentDamageCondition());
    @Override public NexusId type() {
        return DamageModuleConditionIds.HAS_PARENT_DAMAGE;
    }
    @Override public boolean test(DamageContextView ctx) {
        return ctx.hasParentDamage();
    }
}
