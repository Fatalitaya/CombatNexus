package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Tests the current origin's frozen downstream PROC permission. */
public record ProcAllowedCondition() implements DamageRuleCondition {
    public static final MapCodec<ProcAllowedCondition> CODEC =
            MapCodec.unit(new ProcAllowedCondition());
    @Override public NexusId type() {
        return DamageModuleConditionIds.PROC_ALLOWED;
    }
    @Override public boolean test(DamageContextView ctx) {
        return ctx.triggerPolicy().procAllowed();
    }
}
