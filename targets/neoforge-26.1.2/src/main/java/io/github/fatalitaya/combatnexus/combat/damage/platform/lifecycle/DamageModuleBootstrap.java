package io.github.fatalitaya.combatnexus.combat.damage.platform.lifecycle;


import io.github.fatalitaya.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Framework-owned lifecycle coordinator.
 *
 * <p>NeoForge discovers this package-private MOD-bus subscriber directly.
 * Shared initialization remains package-private; this adapter owns the
 * loader event.</p>
 */
@EventBusSubscriber(modid = DamageModule.MODID)
final class DamageModuleBootstrap {

    private static final AtomicBoolean COMMON_SETUP_CLAIMED =
            new AtomicBoolean();

    private DamageModuleBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!claimCommonSetup()) {
            return;
        }

        event.enqueueWork(DamageModuleBootstrap::runCommonSetup);
    }

    static boolean claimCommonSetup() {
        return COMMON_SETUP_CLAIMED.compareAndSet(false, true);
    }

    private static void runCommonSetup() {
        var itemDescriptions = new io.github.fatalitaya.combatnexus.presentation.platform.NativeItemComponentDescriptions.Registration();
        var attributes = new io.github.fatalitaya.combatnexus.attribute.platform.AttributeCapturePolicies.Registration();
        try (itemDescriptions; attributes) {
            io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.initialize(registrar ->
                    NeoForge.EVENT_BUS.post(new io.github.fatalitaya.combatnexus.combat.platform.api.CombatRegisterEvent(registrar, itemDescriptions, attributes)));
        }
        itemDescriptions.publish();
        attributes.publish();
        io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle.DamageModuleInitialization.initialize(session -> {
            io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.registerDamageContributions(session);
            NeoForge.EVENT_BUS.post(new DamageModuleRegisterEvent(session));
        });
    }

}
