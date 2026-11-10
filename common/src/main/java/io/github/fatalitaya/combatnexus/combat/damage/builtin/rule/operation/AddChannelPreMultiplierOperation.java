package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleOperationIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;
import java.util.Optional;

public record AddChannelPreMultiplierOperation(
        NexusId channelId,
        Optional<NexusId> preMultiplierBucketId,
        float value
) implements DamageRuleOperation,
        ChannelReferencingOperation,
        PreMultiplierBucketReferencingOperation {

    public static final MapCodec<AddChannelPreMultiplierOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(AddChannelPreMultiplierOperation::channelId),

                    DamageRuleCodecs.PRE_MULTIPLIER_BUCKET_ID
                            .optionalFieldOf("pre_multiplier_bucket")
                            .forGetter(AddChannelPreMultiplierOperation::preMultiplierBucketId),

                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddChannelPreMultiplierOperation::value)
            ).apply(instance, AddChannelPreMultiplierOperation::new));

    public AddChannelPreMultiplierOperation(
            DamageChannel channel,
            Optional<NexusId> preMultiplierBucketId,
            float value
    ) {
        this(
                DamageOperationChannelIds.idOrUntyped(channel),
                preMultiplierBucketId,
                value
        );
    }

    public AddChannelPreMultiplierOperation {
        channelId = DamageOperationChannelIds.idOrUntyped(channelId);

        if (preMultiplierBucketId == null) {
            preMultiplierBucketId = Optional.empty();
        }
    }

    public DamageChannel channel() {
        return DamageOperationChannelIds.resolve(channelId);
    }

    @Override public NumericLimit numericLimit() { return NumericLimit.MULTIPLIER; }

    @Override
    public NexusId type() {
        return DamageModuleOperationIds.ADD_CHANNEL_PRE_MULTIPLIER;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        int bucketId =
                DamageOperationPreMultiplierBuckets.resolveOrGeneric(
                        preMultiplierBucketId
                );

        return ctx.tryAddChannelPreMultiplier(
                ctx.resolveChannel(channelId),
                bucketId,
                value,
                RuleTraceIds.ADD_CHANNEL_PRE_MULTIPLIER
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(
                DamagePhase.TYPE_SCALING,
                DamagePhase.CRITICAL_HIT,
                DamagePhase.CONDITIONAL_MULTI,
                DamagePhase.GLOBAL_ADJUSTMENT
        );
    }

    @Override
    public List<NexusId> referencedChannels() {
        return List.of(channelId);
    }

    @Override
    public List<NexusId> referencedPreMultiplierBuckets() {
        return preMultiplierBucketId
                .map(List::of)
                .orElseGet(List::of);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
