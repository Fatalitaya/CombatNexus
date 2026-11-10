package io.github.fatalitaya.combatnexus.presentation.platform.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** A screen lifetime, bound to one world session and target. */
public record CombatPanelSubscriptionPayload(UUID subscription, UUID session, int entityId, UUID entity, boolean subscribe)
        implements CustomPacketPayload {
    public static final Type<CombatPanelSubscriptionPayload> TYPE = new Type<>(NativeIds.nativeId(new NexusId("combatnexus", "panel_subscription")));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatPanelSubscriptionPayload> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> { buf.writeUUID(value.subscription()); buf.writeUUID(value.session()); buf.writeVarInt(value.entityId()); buf.writeUUID(value.entity()); buf.writeBoolean(value.subscribe()); },
        buf -> new CombatPanelSubscriptionPayload(buf.readUUID(), buf.readUUID(), buf.readVarInt(), buf.readUUID(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
