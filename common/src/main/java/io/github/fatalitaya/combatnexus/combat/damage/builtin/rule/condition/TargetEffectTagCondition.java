package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;

public record TargetEffectTagCondition(
        NexusId tag
) implements DamageRuleCondition {

    public static final MapCodec<TargetEffectTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.MOB_EFFECT_TAG
                            .fieldOf("tag")
                            .forGetter(TargetEffectTagCondition::tag)
            ).apply(instance, TargetEffectTagCondition::new));

    public TargetEffectTagCondition {
        Objects.requireNonNull(tag, "tag");
    }

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.TARGET_EFFECT_TAG;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.targetFacts().nativeEffectTags().contains(tag);
    }
}
