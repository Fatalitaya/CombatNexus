package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record DamageTypeIsCondition(
        NexusId damageType
) implements DamageRuleCondition {

    public static final MapCodec<DamageTypeIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC
                            .fieldOf("damage_type")
                            .forGetter(DamageTypeIsCondition::damageType)
            ).apply(instance, DamageTypeIsCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.DAMAGE_TYPE_IS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.damageType().filter(damageType::equals).isPresent();
    }
}
