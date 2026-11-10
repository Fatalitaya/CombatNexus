package io.github.fatalitaya.combatnexus.platform;

import io.github.fatalitaya.combatnexus.CombatNexusCommon;

import io.github.fatalitaya.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Loader composition root. Damage is one module of CombatNexus. */
@Mod(CombatNexusCommon.MOD_ID)
public final class CombatNexusNeoForge261 {
    public CombatNexusNeoForge261(IEventBus bus, ModContainer container) {
        io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.install(new NativeContentAccess());
        var attributeAccess = new NativeAttributeAccess();
        io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess.install(attributeAccess);
        io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.install(attributeAccess);
        io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageComponents.install(new NativeComponents());
        io.github.fatalitaya.combatnexus.combat.internal.registration.BuiltinBehaviorTypes.register();
        new DamageModule(bus, container);
        io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents.register(bus);
        io.github.fatalitaya.combatnexus.effect.platform.EnvironmentEffectAdapters.register(bus);
        bus.addListener(io.github.fatalitaya.combatnexus.presentation.platform.CombatPresentationNetwork::register);
    }
}
