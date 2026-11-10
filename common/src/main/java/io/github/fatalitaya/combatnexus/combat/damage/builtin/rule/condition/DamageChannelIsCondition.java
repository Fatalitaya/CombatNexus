package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.ChannelReferencingCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCodecs;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;

public record DamageChannelIsCondition(
        NexusId channelId
) implements DamageRuleCondition, ChannelReferencingCondition {

    public static final MapCodec<DamageChannelIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(DamageChannelIsCondition::channelId)
            ).apply(instance, DamageChannelIsCondition::new));

    public DamageChannelIsCondition(DamageChannel channel) {
        this(channel.id());
    }

    public DamageChannel channel() {
        return DamageChannelRegistry.getChannelOrUntyped(channelId);
    }

    @Override
    public NexusId type() {
        return DamageModuleConditionIds.DAMAGE_CHANNEL_IS;
    }

    @Override
    public boolean test(DamageContextView ctx) {
        return ctx.hasActiveDamageInChannel(ctx.resolveChannel(channelId));
    }

    @Override
    public List<NexusId> referencedChannels() {
        return List.of(channelId);
    }
}
