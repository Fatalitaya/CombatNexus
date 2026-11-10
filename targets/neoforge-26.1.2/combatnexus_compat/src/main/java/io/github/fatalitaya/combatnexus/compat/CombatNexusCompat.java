package io.github.fatalitaya.combatnexus.compat;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import io.github.fatalitaya.combatnexus.combat.platform.api.CombatRegisterEvent;

@Mod("combatnexus_compat")
public final class CombatNexusCompat {
    public CombatNexusCompat() {
        NeoForge.EVENT_BUS.addListener(CombatNexusCompat::register);
        NeoForge.EVENT_BUS.addListener(CombatNexusCompat::registerDamage);
    }
    private static void registerDamage(io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent event) {
        if (ModList.get().isLoaded("apothic_attributes")) {
            event.registerPhaseProcessor(io.github.fatalitaya.combatnexus.api.id.NexusId.parse("apothic_attributes:defense_penetration"),
                    new io.github.fatalitaya.combatnexus.compat.apotheosis.AttributePenetration());
            var id = io.github.fatalitaya.combatnexus.compat.apotheosis.ProjectileDamage.ID;
            event.registerPhaseProcessor(id, new io.github.fatalitaya.combatnexus.compat.apotheosis.ProjectileDamage(
                    event.registrar().registerPreMultiplierBucket(id)));
        }
    }
    private static void register(CombatRegisterEvent event) {
        if (ModList.get().isLoaded("apotheosis"))
            io.github.fatalitaya.combatnexus.compat.apotheosis.ApotheosisBridge.register(event.registrar(), event.attributes());
        if (ModList.get().isLoaded("apothic_attributes"))
            io.github.fatalitaya.combatnexus.compat.apotheosis.AttributesBridge.register(event.registrar());
    }
}
