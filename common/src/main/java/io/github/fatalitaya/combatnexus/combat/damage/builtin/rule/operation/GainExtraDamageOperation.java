package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.ChannelReferencingOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleTraceIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleOperationIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;

public record GainExtraDamageOperation(
        NexusId basedOnChannel,
        NexusId toChannel,
        float ratio
) implements DamageRuleOperation, ChannelReferencingOperation {

    public static final MapCodec<GainExtraDamageOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("based_on_channel")
                            .forGetter(GainExtraDamageOperation::basedOnChannel),

                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("to_channel")
                            .forGetter(GainExtraDamageOperation::toChannel),

                    DamageRuleCodecs.NON_NEGATIVE_FLOAT
                            .fieldOf("ratio")
                            .forGetter(GainExtraDamageOperation::ratio)
            ).apply(instance, GainExtraDamageOperation::new));

    @Override public NumericLimit numericLimit() { return NumericLimit.MULTIPLIER; }

    @Override
    public NexusId type() {
        return DamageModuleOperationIds.GAIN_EXTRA_DAMAGE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryGainExtraDamage(
                ctx.resolveChannel(basedOnChannel),
                ctx.resolveChannel(toChannel),
                ratio,
                RuleTraceIds.GAIN_EXTRA_DAMAGE
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(DamagePhase.TYPE_SCALING);
    }

    @Override
    public List<NexusId> referencedChannels() {
        return List.of(basedOnChannel, toChannel);
    }

    @Override
    public float stackingValue() {
        return ratio;
    }
}
