package io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.CombatNexusCommon;
import io.github.fatalitaya.combatnexus.combat.damage.platform.entity.DamageDummyAttributeSnapshot;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Server-authoritative attribute snapshot for one currently open menu. */
public record DamageDummyAttributesPayload(
        int containerId,
        DamageDummyAttributeSnapshot snapshot
) implements CustomPacketPayload {

    public static final Type<DamageDummyAttributesPayload> TYPE = new Type<>(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(
            NexusId.fromNamespaceAndPath(
                    CombatNexusCommon.MOD_ID,
                    "damage_dummy_attributes"
            )
    ));

    public static final StreamCodec<ByteBuf, DamageDummyAttributesPayload>
            STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DamageDummyAttributesPayload::containerId,
            io.github.fatalitaya.combatnexus.combat.damage.platform.network.DamageDummyCodecs.SNAPSHOT,
            DamageDummyAttributesPayload::snapshot,
            DamageDummyAttributesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
