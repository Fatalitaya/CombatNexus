package io.github.fatalitaya.combatnexus.presentation.platform.network;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.UUID;

/** Complete server-owned panel data for the current screen subscription. */
public record CombatPresentationPayload(UUID subscription, UUID session, NexusId dimension, long generation, int entityId, UUID entity, long incarnation,
                                        long revision, boolean removed, String json) implements CustomPacketPayload {
    public static final int MAX_JSON = 32767;
    public static final Type<CombatPresentationPayload> TYPE = new Type<>(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(NexusId.fromNamespaceAndPath("combatnexus", "presentation_snapshot")));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatPresentationPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUUID(value.subscription()); buf.writeUUID(value.session()); buf.writeResourceLocation(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(value.dimension()));
                buf.writeVarLong(value.generation()); buf.writeVarInt(value.entityId()); buf.writeUUID(value.entity()); buf.writeVarLong(value.incarnation()); buf.writeVarLong(value.revision()); buf.writeBoolean(value.removed());
                buf.writeUtf(value.json(), MAX_JSON);
            }, buf -> new CombatPresentationPayload(buf.readUUID(), buf.readUUID(), io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(buf.readResourceLocation()),
                    buf.readVarLong(), buf.readVarInt(), buf.readUUID(), buf.readVarLong(), buf.readVarLong(), buf.readBoolean(), buf.readUtf(MAX_JSON)));
    public CombatPresentationPayload {
        java.util.Objects.requireNonNull(entity); java.util.Objects.requireNonNull(session);
        java.util.Objects.requireNonNull(dimension); java.util.Objects.requireNonNull(json);
        if (generation < 1 || incarnation < 1 || revision < 0 || json.length() > MAX_JSON) throw new IllegalArgumentException("Invalid projection");
    }
    public boolean fitsEncodingBudget(net.minecraft.core.RegistryAccess registries) {
        var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(256, 65_536), registries);
        try { STREAM_CODEC.encode(buffer, this); return buffer.readableBytes() <= 65_536; }
        catch (RuntimeException failure) {
            return false;
        }
        finally { buffer.release(); }
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
