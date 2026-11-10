package io.github.fatalitaya.combatnexus.combat.damage.platform.item;

import io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions;
import io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions;
import net.minecraft.server.MinecraftServer;

/** Captures the adopted read-only generation for an explicitly supplied server. */
public final class NativeDamageItemDefinitions {
    private NativeDamageItemDefinitions() {}
    public static DamageItemDefinitions current(MinecraftServer server) { return CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (server))); }
}
