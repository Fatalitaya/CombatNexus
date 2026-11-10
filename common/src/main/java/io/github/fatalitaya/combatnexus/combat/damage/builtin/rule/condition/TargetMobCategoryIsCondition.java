package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.entity.EntityCategory;

public record TargetMobCategoryIsCondition(
        EntityCategory category
) implements DamageRuleCondition {

    public static final MapCodec<TargetMobCategoryIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.MOB_CATEGORY
                            .fieldOf("category")
                            .forGetter(TargetMobCategoryIsCondition::category)
            ).apply(instance, TargetMobCategoryIsCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.TARGET_MOB_CATEGORY_IS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.targetFacts() != null
                && ctx.targetFacts().category() == category;
    }
}
