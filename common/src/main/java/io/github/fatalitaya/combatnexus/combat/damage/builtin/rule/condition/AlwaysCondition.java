package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record AlwaysCondition() implements DamageRuleCondition {

    public static final MapCodec<AlwaysCondition> CODEC =
            MapCodec.unit(new AlwaysCondition());

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.ALWAYS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return true;
    }
}
