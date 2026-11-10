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

public record AddChannelMitigationOperation(
        NexusId channelId,
        float value
) implements DamageRuleOperation, ChannelReferencingOperation {

    public static final MapCodec<AddChannelMitigationOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(AddChannelMitigationOperation::channelId),

                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddChannelMitigationOperation::value)
            ).apply(instance, AddChannelMitigationOperation::new));

    public AddChannelMitigationOperation(
            DamageChannel channel,
            float value
    ) {
        this(DamageOperationChannelIds.idOrUntyped(channel), value);
    }

    public AddChannelMitigationOperation {
        channelId = DamageOperationChannelIds.idOrUntyped(channelId);
    }

    @Override
    public NexusId type() {
        return DamageModuleOperationIds.ADD_CHANNEL_MITIGATION;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryAddChannelMitigation(
                ctx.resolveChannel(channelId),
                value,
                RuleTraceIds.ADD_CHANNEL_MITIGATION
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(DamagePhase.MITIGATION_SETUP);
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
