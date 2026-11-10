package io.github.fatalitaya.combatnexus.combat.damage.platform.client.damage;

import io.github.fatalitaya.combatnexus.combat.damage.platform.client.damage.ClientDamageNumberManager;

import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.DamageNumberPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class DamageNumberClientPayloadHandler {

    private DamageNumberClientPayloadHandler() {
    }



    public static void handle(
            DamageNumberPayload payload,
            IPayloadContext context
    ) {
        ClientDamageNumberManager.spawn(payload);
    }
}
