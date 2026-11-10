package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.combat.damage.util.EntityConditionUtil;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record AttackerEntityTypeTagCondition(
        NexusId tag
) implements DamageRuleCondition {

    public static final MapCodec<AttackerEntityTypeTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC
                            .fieldOf("tag")
                            .forGetter(AttackerEntityTypeTagCondition::tag)
            ).apply(instance, AttackerEntityTypeTagCondition::new));

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.ATTACKER_ENTITY_TYPE_TAG;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return EntityConditionUtil.isEntityTypeTag(
                ctx.attackerFacts().map(facts -> facts.entityType()).orElse(null),
                tag
        );
    }
}
