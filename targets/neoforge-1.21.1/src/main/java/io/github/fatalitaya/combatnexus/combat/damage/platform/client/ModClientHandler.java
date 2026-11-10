package io.github.fatalitaya.combatnexus.combat.damage.platform.client;

import io.github.fatalitaya.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.fatalitaya.combatnexus.combat.damage.platform.client.damage.DamageNumberClientPayloadHandler;
import io.github.fatalitaya.combatnexus.combat.damage.platform.client.screen.DamageDummyAttributesClientPayloadHandler;
import io.github.fatalitaya.combatnexus.combat.damage.platform.client.screen.ModMenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = DamageModule.MODID, dist = Dist.CLIENT)
public class ModClientHandler {
    public ModClientHandler(ModContainer container) {
        // The 1.21.1 game bus starts after mod lifecycle events. The first client
        // tick also initializes description capabilities before joining a server.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                net.neoforged.bus.api.EventPriority.HIGHEST,
                (net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) ->
                        io.github.fatalitaya.combatnexus.combat.damage.platform.lifecycle.DamageModuleBootstrap.initialize());
        io.github.fatalitaya.combatnexus.presentation.platform.client.CombatControlClient.register();
        container.getEventBus().addListener(io.github.fatalitaya.combatnexus.presentation.platform.client.CombatPresentationClient::keys);
        container.getEventBus().addListener(
                DamageDummyRenderRegistration::registerLayerDefinitions
        );
        container.getEventBus().addListener(
                DamageDummyRenderRegistration::registerRenderers
        );
        container.getEventBus().addListener(
                ModMenuScreens::registerScreens
        );
    }

}
