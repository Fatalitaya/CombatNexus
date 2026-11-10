package io.github.fatalitaya.combatnexus.combat.damage.platform.lifecycle;


import io.github.fatalitaya.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.concurrent.atomic.AtomicBoolean;

/** Registers extensions once, after the game bus starts and before reload candidates are built. */
public final class DamageModuleBootstrap {

    private static final AtomicBoolean REGISTRATION_CLAIMED =
            new AtomicBoolean();

    private DamageModuleBootstrap() {
    }

    public static void onFirstReload(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        initialize();
    }

    public static void initialize() {
        if (!claimRegistration()) {
            return;
        }

        registerExtensions();
    }

    static boolean claimRegistration() {
        return REGISTRATION_CLAIMED.compareAndSet(false, true);
    }

    private static void registerExtensions() {
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
