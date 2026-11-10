package io.github.fatalitaya.combatnexus.combat.damage.platform.entity;
import io.github.fatalitaya.combatnexus.combat.damage.platform.entity.*;

import io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess;
import io.github.fatalitaya.combatnexus.combat.damage.platform.block.entity.DamageDummyBlockEntity;
import io.github.fatalitaya.combatnexus.combat.damage.platform.menu.DamageDummyMenu;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The single server/common service for resolving, snapshotting and editing a
 * pedestal's real {@link DamageDummyEntity} attribute map.
 */
public final class DamageDummyAttributeService {

    public enum ApplyResult {
        APPLIED,
        INVALID_BATCH
    }

    private DamageDummyAttributeService() {
    }

    public static DamageDummyAttributeSnapshot snapshot(
            ServerLevel level,
            DamageDummyBlockEntity blockEntity
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(blockEntity, "blockEntity");
        BlockPos anchorPos = blockEntity.getBlockPos();
        return blockEntity.resolveManagedDummy(level)
                .map(dummy -> snapshot(anchorPos, dummy))
                .orElseGet(() ->
                        io.github.fatalitaya.combatnexus.combat.damage.platform.network.DamageDummyCodecs.unavailable(anchorPos));
    }

    public static DamageDummyAttributeSnapshot snapshot(
            BlockPos anchorPos,
            DamageDummyEntity dummy
    ) {
        List<DamageDummyAttributeView> views = DamageDummyAttributes
                .availableAttributes(dummy)
                .stream()
                .map(entry -> new DamageDummyAttributeView(
                        entry.id(),
                        entry.attribute().value().getDescriptionId(),
                        entry.baseValue(),
                        entry.value(),
                        entry.attribute().value().getDefaultValue()
                ))
                .toList();
        return new DamageDummyAttributeSnapshot(io.github.fatalitaya.combatnexus.combat.damage.platform.network.DamageDummyCodecs.position(anchorPos), true, views);
    }

    /**
     * Fully validates the batch before touching any instance. Unknown IDs,
     * missing instances, duplicates and non-finite values reject everything.
     */
    public static ApplyResult validateAndApply(
            DamageDummyEntity dummy,
            List<DamageDummyAttributeEdit> edits
    ) {
        Objects.requireNonNull(dummy, "dummy");
        return DamageDummyAttributeBatch.validateAndApply(id -> {
            var holder = AttributeAccess.get().attribute(id).orElse(null);
            if (holder == null) return java.util.Optional.empty();
            var instance = dummy.getAttribute(holder);
            if (instance == null) return java.util.Optional.empty();
            return java.util.Optional.of(new DamageDummyAttributeBatch.AttributeEditor() {
                public double sanitize(double value) { return holder.value().sanitizeValue(value); }
                public void write(double value) { instance.setBaseValue(value); }
            });
        }, edits) ? ApplyResult.APPLIED : ApplyResult.INVALID_BATCH;
    }

    /** Request identity check kept separate so network validation is testable. */
    public static boolean requestMatchesMenu(
            DamageDummyMenu menu,
            int containerId,
            BlockPos anchorPos
    ) {
        return menu != null
                && anchorPos != null
                && menu.containerId == containerId
                && menu.anchorPos().equals(anchorPos);
    }
}
