package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record IsPrimaryDamageCondition() implements DamageRuleCondition {
    public static final MapCodec<IsPrimaryDamageCondition> CODEC =
            MapCodec.unit(new IsPrimaryDamageCondition());
    @Override public NexusId type() {
        return DamageModuleConditionIds.IS_PRIMARY_DAMAGE;
    }
    @Override public boolean test(DamageContextView ctx) {
        return ctx.requestKind() == DamageRequestKind.PRIMARY;
    }
}
