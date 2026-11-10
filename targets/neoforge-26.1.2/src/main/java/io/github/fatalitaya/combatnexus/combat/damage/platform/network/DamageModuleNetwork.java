package io.github.fatalitaya.combatnexus.combat.damage.platform.network;

import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.DamageDummyApplyAttributesPayload;
import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.DamageDummyAttributesPayload;
import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.DamageNumberPayload;
import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.FormulaSettingsPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DamageModuleNetwork {

    private DamageModuleNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // NeoForge requires a non-empty token; this is fixed identity, not a release version.
        event.registrar("combatnexus-description-3")
                .playToClient(FormulaSettingsPayload.TYPE,
                        FormulaSettingsPayload.STREAM_CODEC)
                .playToClient(
                        DamageNumberPayload.TYPE,
                        DamageNumberPayload.STREAM_CODEC
                )
                .playToServer(
                        DamageDummyApplyAttributesPayload.TYPE,
                        DamageDummyApplyAttributesPayload.STREAM_CODEC,
                        DamageDummyAttributePayloadHandler::handleApply
                )
                .playToClient(
                        DamageDummyAttributesPayload.TYPE,
                        DamageDummyAttributesPayload.STREAM_CODEC
                );
    }
}
