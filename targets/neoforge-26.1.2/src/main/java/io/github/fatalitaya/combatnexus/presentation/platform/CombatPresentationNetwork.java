package io.github.fatalitaya.combatnexus.presentation.platform;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.presentation.platform.network.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** World-owned panel subscriptions; native tracking only supplies visibility authorization. */
public final class CombatPresentationNetwork {
    private static final class Viewer {
        final UUID subscription;
        long revision = -1;
        Viewer(UUID subscription) { this.subscription = subscription; }
    }
    private static final class Mirror {
        final Map<ServerPlayer, Viewer> viewers = new IdentityHashMap<>();
        com.google.gson.JsonObject content;
        String json;
        long generation, revision, capturedAt = Long.MIN_VALUE;
    }
    private final Map<LivingEntity, Set<ServerPlayer>> tracking = new IdentityHashMap<>();
    private final Map<LivingEntity, Mirror> mirrors = new IdentityHashMap<>();
    private final Map<ServerPlayer, Integer> controls = new IdentityHashMap<>();
    private long revision;
    private static CombatPresentationNetwork state(ServerLevel level) {
        return io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level).presentationNetwork();
    }
    public void track(LivingEntity target, ServerPlayer viewer) {
        tracking.computeIfAbsent(target, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(viewer);
    }
    public void untrack(LivingEntity target, ServerPlayer viewer) {
        var tracked = tracking.get(target);
        if (tracked != null) { tracked.remove(viewer); if (tracked.isEmpty()) tracking.remove(target); }
        var mirror = mirrors.get(target);
        if (mirror == null) return;
        var subscription = mirror.viewers.remove(viewer);
        if (subscription != null && mirror.generation > 0)
            PacketDistributor.sendToPlayer(viewer, payload(target, subscription.subscription, mirror.generation, ++revision, true, "{}"));
        if (mirror.viewers.isEmpty()) mirrors.remove(target);
    }
    private void unsubscribe(ServerPlayer viewer, UUID subscription) {
        mirrors.values().forEach(mirror -> {
            var current = mirror.viewers.get(viewer);
            if (current != null && (subscription == null || current.subscription.equals(subscription))) mirror.viewers.remove(viewer);
        });
        mirrors.values().removeIf(mirror -> mirror.viewers.isEmpty());
    }
    public void invalidateEntity(UUID entity) {
        for (var target : List.copyOf(tracking.keySet())) if (target.getUUID().equals(entity)) {
            for (var viewer : List.copyOf(tracking.get(target))) untrack(target, viewer);
        }
        for (var target : List.copyOf(mirrors.keySet())) if (target.getUUID().equals(entity)) {
            for (var viewer : List.copyOf(mirrors.get(target).viewers.keySet())) untrack(target, viewer);
        }
        controls.keySet().removeIf(player -> player.getUUID().equals(entity));
    }
    public void reset() { tracking.clear(); mirrors.clear(); controls.clear(); }
    private static void request(CombatPanelSubscriptionPayload request, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        var binding = (io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding)level;
        if (binding.combatnexus$worldSession() == null || !request.session().equals(binding.combatnexus$worldSession().id())) return;
        existing(level).ifPresent(state -> {
            if (!request.subscribe()) { state.unsubscribe(player, request.subscription()); return; }
            state.unsubscribe(player, null);
            if (!(level.getEntity(request.entityId()) instanceof LivingEntity target) || !target.getUUID().equals(request.entity()) || target.isRemoved()) return;
            if (target != player && !state.tracking.getOrDefault(target, Set.of()).contains(player)) {
                var runtime = io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level);
                PacketDistributor.sendToPlayer(player, state.payload(target, request.subscription(), runtime.definitions().generation(), ++state.revision, false,
                    io.github.fatalitaya.combatnexus.presentation.api.CombatPanelProjector.refused("TARGET_NOT_TRACKED").toString()));
                return;
            }
            state.mirrors.computeIfAbsent(target, ignored -> new Mirror()).viewers.put(player, new Viewer(request.subscription()));
        });
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("combatnexus-description-6")
                .playToServer(CombatPanelSubscriptionPayload.TYPE, CombatPanelSubscriptionPayload.STREAM_CODEC, CombatPresentationNetwork::request)
                .playToClient(CombatPresentationPayload.TYPE, CombatPresentationPayload.STREAM_CODEC)
                .playToClient(CombatGenerationPayload.TYPE, CombatGenerationPayload.STREAM_CODEC)
                .playToClient(TooltipDefinitionPayload.TYPE, TooltipDefinitionPayload.STREAM_CODEC)
                .playToClient(EffectTooltipPayload.TYPE, EffectTooltipPayload.STREAM_CODEC)
                .playToClient(CombatControlPayload.TYPE, CombatControlPayload.STREAM_CODEC);
    }
    public static void flushControls(ServerLevel level) {
        var state = state(level);
        var session = io.github.fatalitaya.combatnexus.combat.api.CombatWorldSessions.current((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) level).orElseThrow();
        for (var player : level.players()) {
            if (!player.connection.hasChannel(CombatControlPayload.TYPE)) continue;
            int mask = io.github.fatalitaya.combatnexus.combat.api.CombatWorldSessions.restrictionMask((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) level, player.getUUID());
            if (!Objects.equals(state.controls.get(player), mask)) {
                PacketDistributor.sendToPlayer(player, new CombatControlPayload(session, NativeIds.nexus(level.dimension().identifier()), player.getId(), ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance)player).combatnexus$incarnation(), level.getGameTime(), mask));
                state.controls.put(player, mask);
            }
        }
    }
    public static void flushMirrors(ServerLevel level) {
        var state = state(level);
        var runtime = io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level);
        long generation = runtime.serverRuntime().definitions().orElseThrow().generation();
        for (var entry : List.copyOf(state.mirrors.entrySet())) {
            var entity = entry.getKey(); var mirror = entry.getValue();
            mirror.viewers.keySet().removeIf(player -> player.hasDisconnected() || player.level() != level || !level.players().contains(player));
            if (entity.isRemoved() || entity.level() != level || mirror.viewers.isEmpty()) { state.mirrors.remove(entity); continue; }
            boolean first = mirror.viewers.values().stream().anyMatch(viewer -> viewer.revision < 0);
            if (!first && generation == mirror.generation && level.getGameTime() - mirror.capturedAt < 10) continue;
            com.google.gson.JsonObject content;
            try { content = NativeEntityCombatMirror.capture(runtime, entity); }
            catch (io.github.fatalitaya.combatnexus.presentation.api.PresentationBudget.Exceeded failure) { content = io.github.fatalitaya.combatnexus.presentation.api.CombatPanelProjector.refused("BUDGET_EXCEEDED"); }
            catch (RuntimeException failure) { content = io.github.fatalitaya.combatnexus.presentation.api.CombatPanelProjector.refused("CAPTURE_FAILED"); }
            if (!content.equals(mirror.content) || generation != mirror.generation) {
                String json = content.toString();
                if (json.length() > CombatPresentationPayload.MAX_JSON) json = io.github.fatalitaya.combatnexus.presentation.api.CombatPanelProjector.refused("BUDGET_EXCEEDED").toString();
                mirror.revision = ++state.revision;
                if (!state.payload(entity, new UUID(0, 0), generation, mirror.revision, false, json).fitsEncodingBudget(level.registryAccess()))
                    json = io.github.fatalitaya.combatnexus.presentation.api.CombatPanelProjector.refused("ENCODING_FAILED").toString();
                mirror.content = content; mirror.json = json; mirror.generation = generation;
            }
            mirror.capturedAt = level.getGameTime();
            for (var viewer : mirror.viewers.entrySet()) if (viewer.getValue().revision != mirror.revision) {
                PacketDistributor.sendToPlayer(viewer.getKey(), state.payload(entity, viewer.getValue().subscription, generation, mirror.revision, false, mirror.json));
                viewer.getValue().revision = mirror.revision;
            }
        }
    }
    private CombatPresentationPayload payload(LivingEntity entity, UUID subscription, long generation, long revision, boolean removed, String json) {
        var level = (ServerLevel)entity.level();
        var session = ((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding)level).combatnexus$worldSession().id();
        return new CombatPresentationPayload(subscription, session, NativeIds.nexus(level.dimension().identifier()), generation,
            entity.getId(), entity.getUUID(), ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance)entity).combatnexus$incarnation(), revision, removed, json);
    }
    public static void clear(ServerPlayer player, ServerLevel level) { existing(level).ifPresent(state -> { state.unsubscribe(player, null); state.tracking.values().forEach(viewers -> viewers.remove(player)); state.tracking.values().removeIf(Set::isEmpty); state.invalidateEntity(player.getUUID()); }); }
    public static void clear(ServerLevel level) { existing(level).ifPresent(CombatPresentationNetwork::reset); }
    private static Optional<CombatPresentationNetwork> existing(ServerLevel level) {
        var binding = (io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding)level;
        return io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(binding.combatnexus$serverBinding())
            .flatMap(server -> server.world(binding.combatnexus$worldSession())).map(world -> world.presentationNetwork());
    }
}
