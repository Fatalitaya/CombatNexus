package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record DamageTypeTagCondition(
        NexusId tag
) implements DamageRuleCondition {

    public static final MapCodec<DamageTypeTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC
                            .fieldOf("tag")
                            .forGetter(DamageTypeTagCondition::tag)
            ).apply(instance, DamageTypeTagCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.DAMAGE_TYPE_TAG;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.damageTypeTags().contains(tag);
    }
}
