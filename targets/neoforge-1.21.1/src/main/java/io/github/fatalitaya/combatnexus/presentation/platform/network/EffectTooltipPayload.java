package io.github.fatalitaya.combatnexus.presentation.platform.network;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Adopted execution description for one projected effect. Empty JSON revokes it. */
public record EffectTooltipPayload(UUID target, UUID session, long generation, NexusId dimension, NexusId effect, String json) implements CustomPacketPayload {
    public static final Type<EffectTooltipPayload> TYPE = new Type<>(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(NexusId.parse("combatnexus:effect_tooltip")));
    public static final StreamCodec<FriendlyByteBuf, EffectTooltipPayload> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> { buf.writeUUID(value.target()); buf.writeUUID(value.session()); buf.writeVarLong(value.generation()); buf.writeResourceLocation(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(value.dimension())); buf.writeResourceLocation(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(value.effect())); buf.writeUtf(value.json(), 24000); },
        buf -> new EffectTooltipPayload(buf.readUUID(), buf.readUUID(), buf.readVarLong(), io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(buf.readResourceLocation()), io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(buf.readResourceLocation()), buf.readUtf(24000)));
    public EffectTooltipPayload { if (generation < 1 || json.length() > 24000) throw new IllegalArgumentException("Effect tooltip exceeds packet budget"); }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
