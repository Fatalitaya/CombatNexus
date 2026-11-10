package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record AttackerHealthBelowCondition(
        float threshold
) implements DamageRuleCondition {

    public AttackerHealthBelowCondition { HealthRatio.require(threshold); }

    public static final MapCodec<AttackerHealthBelowCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    HealthRatio.CODEC
                            .fieldOf("threshold")
                            .forGetter(AttackerHealthBelowCondition::threshold)
            ).apply(instance, AttackerHealthBelowCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.ATTACKER_HEALTH_BELOW;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        if (ctx.attackerFacts().isEmpty()) {
            return false;
        }

        float maxHealth = ctx.attackerFacts().orElseThrow().maxHealth();

        if (maxHealth <= 0.0f) {
            return false;
        }

        float ratio = ctx.attackerFacts().orElseThrow().health() / maxHealth;
        return ratio < threshold;
    }
}
