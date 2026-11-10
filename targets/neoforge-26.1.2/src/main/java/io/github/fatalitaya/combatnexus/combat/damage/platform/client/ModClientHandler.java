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
        container.getEventBus().addListener(io.github.fatalitaya.combatnexus.presentation.platform.client.CombatPresentationClient::register);
        container.getEventBus().addListener(io.github.fatalitaya.combatnexus.presentation.platform.client.CombatPresentationClient::keys);
        container.getEventBus().addListener(FormulaSettingsClientHandler::register);

        container.getEventBus().addListener(
                DamageNumberClientPayloadHandler
                        ::onRegisterPayloadHandlers
        );
        container.getEventBus().addListener(
                DamageDummyAttributesClientPayloadHandler
                        ::onRegisterPayloadHandlers
        );
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
