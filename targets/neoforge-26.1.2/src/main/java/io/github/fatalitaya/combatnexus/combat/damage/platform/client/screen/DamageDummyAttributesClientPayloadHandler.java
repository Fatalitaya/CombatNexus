package io.github.fatalitaya.combatnexus.combat.damage.platform.client.screen;

import io.github.fatalitaya.combatnexus.combat.damage.platform.client.screen.DamageDummyScreen;

import io.github.fatalitaya.combatnexus.combat.damage.platform.menu.DamageDummyMenu;
import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.DamageDummyAttributesPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Physical-client-only receiver for authoritative menu snapshots. */
public final class DamageDummyAttributesClientPayloadHandler {

    private DamageDummyAttributesClientPayloadHandler() {
    }

    public static void onRegisterPayloadHandlers(
            RegisterClientPayloadHandlersEvent event
    ) {
        event.register(
                DamageDummyAttributesPayload.TYPE,
                DamageDummyAttributesClientPayloadHandler::handle
        );
    }

    public static void handle(
            DamageDummyAttributesPayload payload,
            IPayloadContext context
    ) {
        if (!(Minecraft.getInstance().screen
                instanceof DamageDummyScreen screen)) {
            return;
        }
        DamageDummyMenu menu = screen.getMenu();
        if (menu.containerId != payload.containerId()
                || payload.snapshot() == null
                || !menu.anchorPos().equals(payload.snapshot().anchorPos())) {
            return;
        }
        if (menu.replaceSnapshot(payload.snapshot())) {
            screen.refreshFromSnapshot();
        }
    }
}
