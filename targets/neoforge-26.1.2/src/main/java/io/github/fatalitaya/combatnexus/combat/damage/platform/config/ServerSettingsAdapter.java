package io.github.fatalitaya.combatnexus.combat.damage.platform.config;

import io.github.fatalitaya.combatnexus.platform.NativeCombatReloadListener;
import io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions;
import io.github.fatalitaya.combatnexus.combat.damage.core.config.DamageSettingsStore;
import io.github.fatalitaya.combatnexus.combat.damage.config.VanillaCompatibilitySettings;
import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.FormulaSettingsPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.resource.ListenerKey;

/** Retained listeners belong to one reload; publish only once the server adopts those resources. */
@EventBusSubscriber(modid = "combatnexus")
public final class ServerSettingsAdapter {
    private static final ListenerKey<NativeCombatReloadListener> KEY = ListenerKey.create(
            Identifier.fromNamespaceAndPath("combatnexus", "settings"));
    private ServerSettingsAdapter() {}
    @SubscribeEvent public static void register(AddServerReloadListenersEvent event) {
        event.addRetainedListener(KEY, new NativeCombatReloadListener(event.getServerResources().getRegistryLookup()));
    }
    @SubscribeEvent public static void start(ServerAboutToStartEvent event) {
        var server = event.getServer();
        var runtime = new io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime(
                new io.github.fatalitaya.combatnexus.combat.api.transaction.ServerSession(), server::isSameThread, server::getTickCount);
        ((io.github.fatalitaya.combatnexus.combat.platform.NativeServerRuntimeBinding) server).combatnexus$bindServerRuntime(runtime);
        commit(server);
    }
    private static DamageSettingsStore.Snapshot commit(MinecraftServer server) {
        var candidate = server.getServerResources().managers().getListener(KEY).candidate();
        CombatDefinitions.publish(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) server), candidate);
        return DamageSettingsStore.current(CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (server))));
    }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {
        var server = event.getPlayerList().getServer();
        // A single-player sync is login, not resource adoption. It must never republish
        // the retained candidate or replace a newer adopted server generation.
        var snapshot = event.getPlayer() == null ? commit(server) : DamageSettingsStore.current(CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (server))));
        var formulas = snapshot.values().formulas();
        var compatibility = snapshot.values().vanillaCompatibility();
        var payload = new FormulaSettingsPayload(snapshot.revision(), formulas.asymptoticKValue(),
                formulas.resistanceKValue(), formulas.ratingPerProtScore(),
                compatibility.executor(VanillaCompatibilitySettings.Reduction.ARMOR)
                        == VanillaCompatibilitySettings.Executor.COMBATNEXUS,
                compatibility.executor(VanillaCompatibilitySettings.Reduction.ENCHANTMENTS)
                        == VanillaCompatibilitySettings.Executor.COMBATNEXUS);
        var generation = new io.github.fatalitaya.combatnexus.presentation.platform.network.CombatGenerationPayload(CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (server))).generation());
        var tooltipDefinitions = TooltipDefinitionSync.capture(server);
        event.getRelevantPlayers().forEach(player -> {
            if (!player.connection.hasChannel(generation.type())) return;
            PacketDistributor.sendToPlayer(player, generation);
            io.github.fatalitaya.combatnexus.effect.platform.NativeEffectBridge.synchronizeDescriptions(player, java.util.List.of());
            PacketDistributor.sendToPlayer(player, payload);
            tooltipDefinitions.forEach(definition -> PacketDistributor.sendToPlayer(player, definition));
        });
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) event.getServer()).ifPresent(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime::clearDefinitions); }
}
