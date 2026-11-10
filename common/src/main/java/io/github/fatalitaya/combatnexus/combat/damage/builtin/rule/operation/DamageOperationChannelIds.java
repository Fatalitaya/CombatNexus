package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation;

import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

final class DamageOperationChannelIds {

    private DamageOperationChannelIds() {
    }

    static NexusId idOrUntyped(DamageChannel channel) {
        return channel == null
                ? DamageChannel.UNTYPED_ID
                : channel.id();
    }

    static NexusId idOrUntyped(NexusId channelId) {
        return channelId == null
                ? DamageChannel.UNTYPED_ID
                : channelId;
    }

    static DamageChannel resolve(NexusId channelId) {
        return DamageChannelRegistry.getChannelOrUntyped(
                idOrUntyped(channelId)
        );
    }
}
