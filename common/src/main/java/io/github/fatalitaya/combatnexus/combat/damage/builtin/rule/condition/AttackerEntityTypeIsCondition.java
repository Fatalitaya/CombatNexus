package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.combat.damage.util.EntityConditionUtil;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record AttackerEntityTypeIsCondition(
        NexusId entityType
) implements DamageRuleCondition {

    public static final MapCodec<AttackerEntityTypeIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC
                            .fieldOf("entity_type")
                            .forGetter(AttackerEntityTypeIsCondition::entityType)
            ).apply(instance, AttackerEntityTypeIsCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.ATTACKER_ENTITY_TYPE_IS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return EntityConditionUtil.isEntityType(
                ctx.attackerFacts().map(facts -> facts.entityType()).orElse(null),
                entityType
        );
    }
}
