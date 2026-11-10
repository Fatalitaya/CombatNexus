package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record TargetHealthAboveCondition(
        float threshold
) implements DamageRuleCondition {

    public TargetHealthAboveCondition { HealthRatio.require(threshold); }

    public static final MapCodec<TargetHealthAboveCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    HealthRatio.CODEC
                            .fieldOf("threshold")
                            .forGetter(TargetHealthAboveCondition::threshold)
            ).apply(instance, TargetHealthAboveCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.TARGET_HEALTH_ABOVE;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        if (ctx.targetFacts() == null) {
            return false;
        }

        float max = ctx.targetFacts().maxHealth();
        if (max <= 0.0f) {
            return false;
        }

        return ctx.targetFacts().health() / max > threshold;
    }
}
