package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.ChannelReferencingOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleTraceIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleOperationIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;

public record AddChannelPostMultiplierOperation(
        NexusId channelId,
        float value
) implements DamageRuleOperation, ChannelReferencingOperation {

    public static final MapCodec<AddChannelPostMultiplierOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(AddChannelPostMultiplierOperation::channelId),

                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddChannelPostMultiplierOperation::value)
            ).apply(instance, AddChannelPostMultiplierOperation::new));

    public AddChannelPostMultiplierOperation(
            DamageChannel channel,
            float value
    ) {
        this(DamageOperationChannelIds.idOrUntyped(channel), value);
    }

    public AddChannelPostMultiplierOperation {
        channelId = DamageOperationChannelIds.idOrUntyped(channelId);
    }

    public DamageChannel channel() {
        return DamageOperationChannelIds.resolve(channelId);
    }

    @Override public NumericLimit numericLimit() { return NumericLimit.MULTIPLIER; }

    @Override
    public NexusId type() {
        return DamageModuleOperationIds.ADD_CHANNEL_POST_MULTIPLIER;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryAddChannelPostMultiplier(
                ctx.resolveChannel(channelId),
                value,
                RuleTraceIds.ADD_CHANNEL_POST_MULTIPLIER
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(
                DamagePhase.CONDITIONAL_MULTI,
                DamagePhase.GLOBAL_ADJUSTMENT
        );
    }

    @Override
    public List<NexusId> referencedChannels() {
        return List.of(channelId);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
