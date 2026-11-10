package io.github.fatalitaya.combatnexus.presentation.platform.network;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** Server-owned input restrictions for one player/world; never a client combat decision. */
public record CombatControlPayload(UUID session, NexusId dimension, int entityId, long incarnation, long tick, int mask) implements CustomPacketPayload {
    public CombatControlPayload { if (incarnation <= 0 || tick < 0 || mask < 0 || mask > 255) throw new IllegalArgumentException("Invalid control state"); }
    public static final Type<CombatControlPayload> TYPE = new Type<>(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(NexusId.fromNamespaceAndPath("combatnexus", "control_state")));
    public static final StreamCodec<FriendlyByteBuf, CombatControlPayload> STREAM_CODEC = StreamCodec.of(
        (buf, v) -> { buf.writeUUID(v.session()); buf.writeIdentifier(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(v.dimension())); buf.writeVarInt(v.entityId()); buf.writeVarLong(v.incarnation()); buf.writeVarLong(v.tick()); buf.writeByte(v.mask()); },
        buf -> new CombatControlPayload(buf.readUUID(), io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(buf.readIdentifier()), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readUnsignedByte()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
