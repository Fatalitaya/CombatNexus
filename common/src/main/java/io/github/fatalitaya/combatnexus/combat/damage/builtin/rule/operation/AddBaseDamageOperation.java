package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.ChannelReferencingOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleTraceIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleOperationIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;

public record AddBaseDamageOperation(
        NexusId channelId,
        DamageApplicationBucket applicationBucket,
        float value
) implements DamageRuleOperation, ChannelReferencingOperation {

    public static final MapCodec<AddBaseDamageOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(AddBaseDamageOperation::channelId),

                    DamageRuleCodecs.DAMAGE_APPLICATION_BUCKET
                            .optionalFieldOf(
                                    "application_bucket",
                                    DamageApplicationBucket.DN_RULE_BASE
                            )
                            .forGetter(AddBaseDamageOperation::applicationBucket),

                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddBaseDamageOperation::value)
            ).apply(instance, AddBaseDamageOperation::new));

    public AddBaseDamageOperation(
            NexusId channelId,
            float value
    ) {
        this(channelId, DamageApplicationBucket.DN_RULE_BASE, value);
    }

    public AddBaseDamageOperation(
            DamageChannel channel,
            float value
    ) {
        this(
                DamageOperationChannelIds.idOrUntyped(channel),
                DamageApplicationBucket.DN_RULE_BASE,
                value
        );
    }

    public AddBaseDamageOperation(
            DamageChannel channel,
            DamageApplicationBucket applicationBucket,
            float value
    ) {
        this(
                DamageOperationChannelIds.idOrUntyped(channel),
                applicationBucket,
                value
        );
    }

    public AddBaseDamageOperation {
        channelId = DamageOperationChannelIds.idOrUntyped(channelId);

        if (applicationBucket == null) {
            applicationBucket = DamageApplicationBucket.DN_RULE_BASE;
        }
    }

    @Override
    public NexusId type() {
        return DamageModuleOperationIds.ADD_BASE_DAMAGE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryAddBaseDamage(
                ctx.resolveChannel(channelId),
                applicationBucket,
                value,
                RuleTraceIds.ADD_BASE_DAMAGE
        );
    }

    @Override
    public List<NexusId> referencedChannels() {
        return List.of(channelId);
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(DamagePhase.BASE_MODIFICATION);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
