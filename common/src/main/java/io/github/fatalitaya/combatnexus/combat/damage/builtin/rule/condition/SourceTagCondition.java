package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;

public record SourceTagCondition(
        NexusId tag
) implements DamageRuleCondition {
    public static final MapCodec<SourceTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.fieldOf("tag")
                            .forGetter(SourceTagCondition::tag)
            ).apply(instance, SourceTagCondition::new));

    public SourceTagCondition {
        Objects.requireNonNull(tag, "tag");
    }

    @Override public NexusId type() {
        return DamageModuleConditionIds.SOURCE_TAG;
    }

    @Override public boolean test(DamageContextView ctx) {
        return ctx.sourceTags().contains(tag);
    }
}
