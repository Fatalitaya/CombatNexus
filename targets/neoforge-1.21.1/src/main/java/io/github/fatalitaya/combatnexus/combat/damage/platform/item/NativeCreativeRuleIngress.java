package io.github.fatalitaya.combatnexus.combat.damage.platform.item;

import io.github.fatalitaya.combatnexus.combat.damage.api.item.*;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.item.ItemStack;

/** Native packet permissions and component mutation; validation belongs to common. */
public final class NativeCreativeRuleIngress {
    private NativeCreativeRuleIngress() {}
    public static void sanitizeCreativeInbound(ServerPlayer player, ItemStack submitted) {
        if (player == null) return;
        if (player.level().getServer() == null || !player.level().getServer().isSameThread())
            throw new IllegalStateException("Creative ItemStack security must run on the Minecraft server thread");
        if (submitted == null || submitted.isEmpty()) return;
        var item = new NativeRuleItem(submitted);
        var patch=io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents.PATCH.get();
        if(!submitted.has(patch))return;
        var definitions=CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding)player.level().getServer()));
        if(!player.hasPermissions(2)){submitted.remove(patch);return;}
        try{DamageChannelRegistry.withCandidate(definitions.damage().channels(),()->definitions.bindings(item));}
        catch(RuntimeException failure){submitted.remove(patch);}
    }
}
