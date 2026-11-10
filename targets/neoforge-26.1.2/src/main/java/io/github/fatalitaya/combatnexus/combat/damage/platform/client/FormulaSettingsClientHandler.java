package io.github.fatalitaya.combatnexus.combat.damage.platform.client;

import io.github.fatalitaya.combatnexus.combat.damage.client.ClientFormulaSettings;

import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.FormulaSettingsPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = "combatnexus", value = Dist.CLIENT)
public final class FormulaSettingsClientHandler {
    private FormulaSettingsClientHandler() {}
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(FormulaSettingsPayload.TYPE, (payload, context) -> ClientFormulaSettings.accept(new ClientFormulaSettings.Settings(payload.revision(), payload.formulas(), payload.managedArmor(), payload.managedEnchantments())));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientFormulaSettings.clear(); }
}
