package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record AttackerHealthAboveCondition(
        float threshold
) implements DamageRuleCondition {

    public AttackerHealthAboveCondition { HealthRatio.require(threshold); }

    public static final MapCodec<AttackerHealthAboveCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    HealthRatio.CODEC
                            .fieldOf("threshold")
                            .forGetter(AttackerHealthAboveCondition::threshold)
            ).apply(instance, AttackerHealthAboveCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.ATTACKER_HEALTH_ABOVE;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.attackerFacts().map(facts -> facts.maxHealth() > 0.0f && facts.health() / facts.maxHealth() > threshold).orElse(false);
    }
}
