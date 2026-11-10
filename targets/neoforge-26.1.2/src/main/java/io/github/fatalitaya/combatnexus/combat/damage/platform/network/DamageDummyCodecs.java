package io.github.fatalitaya.combatnexus.combat.damage.platform.network;
import io.github.fatalitaya.combatnexus.combat.damage.platform.entity.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.*;
import java.util.List;
import static io.github.fatalitaya.combatnexus.combat.damage.platform.entity.DamageDummyAttributeProtocol.MAX_TEXT_LENGTH;
/** Existing bounded wire layouts, owned by the active target. */
public final class DamageDummyCodecs {
    public static final StreamCodec<ByteBuf, NexusId> IDENTIFIER_CODEC =
            ByteBufCodecs.stringUtf8(MAX_TEXT_LENGTH).map(
                    NexusId::parse,
                    NexusId::toString
            );
    public static final StreamCodec<ByteBuf, String> TRANSLATION_KEY_CODEC =
            ByteBufCodecs.stringUtf8(MAX_TEXT_LENGTH);
    public static final StreamCodec<ByteBuf, DamageDummyAttributeView>
            VIEW = StreamCodec.composite(
            IDENTIFIER_CODEC,
            DamageDummyAttributeView::id,
            TRANSLATION_KEY_CODEC,
            DamageDummyAttributeView::translationKey,
            ByteBufCodecs.DOUBLE,
            DamageDummyAttributeView::baseValue,
            ByteBufCodecs.DOUBLE,
            DamageDummyAttributeView::effectiveValue,
            ByteBufCodecs.DOUBLE,
            DamageDummyAttributeView::defaultValue,
            DamageDummyAttributeView::new
    );
    public static final StreamCodec<ByteBuf, DamageDummyAttributeEdit>
            EDIT = StreamCodec.composite(
            IDENTIFIER_CODEC,
            DamageDummyAttributeEdit::attributeId,
            ByteBufCodecs.DOUBLE,
            DamageDummyAttributeEdit::requestedBaseValue,
            DamageDummyAttributeEdit::new
    );
    private static final StreamCodec<ByteBuf, List<DamageDummyAttributeView>>
            ATTRIBUTE_LIST_CODEC = VIEW.apply(
            ByteBufCodecs.list(DamageDummyAttributeProtocol.MAX_ATTRIBUTES)
    );
    public static final StreamCodec<ByteBuf, DamageDummyAttributeSnapshot>
            SNAPSHOT = StreamCodec.composite(
            BlockPos.STREAM_CODEC.map(DamageDummyCodecs::position, DamageDummyCodecs::nativePosition),
            DamageDummyAttributeSnapshot::anchorPos,
            ByteBufCodecs.BOOL,
            DamageDummyAttributeSnapshot::available,
            ATTRIBUTE_LIST_CODEC,
            DamageDummyAttributeSnapshot::attributes,
            DamageDummyAttributeSnapshot::new
    );

    public static DamageDummyAttributeSnapshot read(ByteBuf buffer) { return SNAPSHOT.decode(buffer); }
    public static void write(ByteBuf buffer, DamageDummyAttributeSnapshot snapshot) { SNAPSHOT.encode(buffer, snapshot); }
    public static io.github.fatalitaya.combatnexus.api.geometry.BlockPosition position(BlockPos value) {
        return new io.github.fatalitaya.combatnexus.api.geometry.BlockPosition(value.getX(), value.getY(), value.getZ());
    }
    public static BlockPos nativePosition(io.github.fatalitaya.combatnexus.api.geometry.BlockPosition value) {
        return new BlockPos(value.x(), value.y(), value.z());
    }
    public static DamageDummyAttributeSnapshot unavailable(BlockPos position) {
        return DamageDummyAttributeSnapshot.unavailable(position(position));
    }
}
