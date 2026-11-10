package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;

public record SourceActionIsCondition(
        NexusId action
) implements DamageRuleCondition {
    public static final MapCodec<SourceActionIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.fieldOf("action")
                            .forGetter(SourceActionIsCondition::action)
            ).apply(instance, SourceActionIsCondition::new));

    public SourceActionIsCondition {
        Objects.requireNonNull(action, "action");
    }

    @Override public NexusId type() {
        return DamageModuleConditionIds.SOURCE_ACTION_IS;
    }

    @Override public boolean test(DamageContextView ctx) {
        return ctx.actionId().filter(action::equals).isPresent();
    }
}
