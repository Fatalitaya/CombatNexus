package io.github.fatalitaya.combatnexus.combat.damage.platform.network;

import io.github.fatalitaya.combatnexus.combat.damage.platform.block.entity.DamageDummyBlockEntity;
import io.github.fatalitaya.combatnexus.combat.damage.platform.entity.DamageDummyAttributeService;
import io.github.fatalitaya.combatnexus.combat.damage.platform.entity.DamageDummyAttributeSnapshot;
import io.github.fatalitaya.combatnexus.combat.damage.platform.entity.DamageDummyEntity;
import io.github.fatalitaya.combatnexus.combat.damage.platform.menu.DamageDummyMenu;
import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.DamageDummyApplyAttributesPayload;
import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.DamageDummyAttributesPayload;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Main-thread, fail-closed server handler for attribute edit batches. */
public final class DamageDummyAttributeEditor {

    private final java.util.function.BiConsumer<ServerPlayer, DamageDummyAttributesPayload> sender;
    public DamageDummyAttributeEditor(java.util.function.BiConsumer<ServerPlayer, DamageDummyAttributesPayload> sender) {
        this.sender = java.util.Objects.requireNonNull(sender, "sender");
    }

    public void handleApply(
            DamageDummyApplyAttributesPayload payload,
            ServerPlayer player
    ) {
        if (!(player.containerMenu instanceof DamageDummyMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            sendCurrent(player, menu);
            return;
        }
        if (payload.anchorPos() == null
                || !menu.anchorPos().equals(payload.anchorPos())) {
            sendCurrent(player, menu);
            return;
        }
        if (!menu.stillValid(player)) {
            sendCurrent(player, menu);
            return;
        }

        ServerLevel level = player.serverLevel();
        if (!(level.getBlockEntity(menu.anchorPos())
                instanceof DamageDummyBlockEntity blockEntity)) {
            sendUnavailable(player, menu);
            return;
        }
        Optional<DamageDummyEntity> resolved =
                blockEntity.resolveManagedDummy(level);
        if (resolved.isEmpty()) {
            sendUnavailable(player, menu);
            return;
        }

        DamageDummyAttributeService.ApplyResult result =
                DamageDummyAttributeService.validateAndApply(
                        resolved.get(),
                        payload.edits()
                );
        if (result != DamageDummyAttributeService.ApplyResult.APPLIED) {
            sendCurrent(player, menu);
            return;
        }

        DamageDummyAttributeSnapshot snapshot =
                DamageDummyAttributeService.snapshot(level, blockEntity);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.containerMenu instanceof DamageDummyMenu viewerMenu
                    && viewerMenu.anchorPos().equals(menu.anchorPos())
                    && viewerMenu.stillValid(viewer)) {
                viewerMenu.replaceSnapshot(snapshot);
                sender.accept(
                        viewer,
                        new DamageDummyAttributesPayload(
                                viewerMenu.containerId,
                                snapshot
                        )
                );
            }
        }
    }

    private void sendCurrent(
            ServerPlayer player,
            DamageDummyMenu menu
    ) {
        ServerLevel level = player.serverLevel();
        DamageDummyAttributeSnapshot snapshot;
        if (level.getBlockEntity(menu.anchorPos())
                instanceof DamageDummyBlockEntity blockEntity) {
            snapshot = DamageDummyAttributeService.snapshot(level, blockEntity);
        } else {
            snapshot = io.github.fatalitaya.combatnexus.combat.damage.platform.network.DamageDummyCodecs.unavailable(
                    menu.anchorPos()
            );
        }
        send(player, menu, snapshot);
    }

    private void sendUnavailable(
            ServerPlayer player,
            DamageDummyMenu menu
    ) {
        send(
                player,
                menu,
                io.github.fatalitaya.combatnexus.combat.damage.platform.network.DamageDummyCodecs.unavailable(menu.anchorPos())
        );
    }

    private void send(
            ServerPlayer player,
            DamageDummyMenu menu,
            DamageDummyAttributeSnapshot snapshot
    ) {
        menu.replaceSnapshot(snapshot);
        sender.accept(
                player,
                new DamageDummyAttributesPayload(menu.containerId, snapshot)
        );
    }
}
