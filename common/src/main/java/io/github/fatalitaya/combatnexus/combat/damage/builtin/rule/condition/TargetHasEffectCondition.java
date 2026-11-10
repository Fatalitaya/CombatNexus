package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Optional;

public record TargetHasEffectCondition(
        NexusId effect
) implements DamageRuleCondition {

    public static final MapCodec<TargetHasEffectCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC
                            .fieldOf("effect")
                            .forGetter(TargetHasEffectCondition::effect)
            ).apply(instance, TargetHasEffectCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.TARGET_HAS_EFFECT;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        if (ctx.targetFacts() == null) {
            return false;
        }

        return ctx.targetFacts().hasEffect(effect);
    }
}
