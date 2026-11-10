package io.github.fatalitaya.combatnexus.presentation.platform;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.presentation.platform.network.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** World-owned supplements for every tracked living entity, independent of any screen. */
public final class CombatPresentationNetwork {
    private static final class Mirror {
        final Map<ServerPlayer, Long> viewers = new IdentityHashMap<>();
        String json;
        long generation, revision;
    }
    private final Map<LivingEntity, Mirror> mirrors = new IdentityHashMap<>();
    private final Map<ServerPlayer, Integer> controls = new IdentityHashMap<>();
    private long revision;
    private static CombatPresentationNetwork state(ServerLevel level) {
        return io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level).presentationNetwork();
    }
    public void track(LivingEntity target, ServerPlayer viewer) {
        mirrors.computeIfAbsent(target, ignored -> new Mirror()).viewers.putIfAbsent(viewer, -1L);
    }
    public void untrack(LivingEntity target, ServerPlayer viewer) {
        var mirror = mirrors.get(target);
        if (mirror == null || mirror.viewers.remove(viewer) == null) return;
        if (mirror.generation > 0) PacketDistributor.sendToPlayer(viewer, payload(target, mirror.generation, ++revision, true, "{}"));
        if (mirror.viewers.isEmpty()) mirrors.remove(target);
    }
    public void invalidateEntity(UUID entity) {
        mirrors.forEach((target, mirror) -> { if (target.getUUID().equals(entity)) mirror.json = null; });
        controls.keySet().removeIf(player -> player.getUUID().equals(entity));
    }
    public void reset() { mirrors.clear(); controls.clear(); }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("combatnexus-description-6")
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
            // Vanilla tracking does not pair a player with itself; only recipient registration differs.
            state.track(player, player);
            int mask = io.github.fatalitaya.combatnexus.combat.api.CombatWorldSessions.restrictionMask((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) level, player.getUUID());
            if (!Objects.equals(state.controls.get(player), mask)) {
                PacketDistributor.sendToPlayer(player, new CombatControlPayload(session, NativeIds.nexus(level.dimension().identifier()), player.getId(), ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance)player).combatnexus$incarnation(), level.getGameTime(), mask));
                state.controls.put(player, mask);
            }
        }
    }
    public static void flushMirrors(ServerLevel level) {
        if (level.getGameTime() % 10 != 0) return;
        var state = state(level);
        var runtime = io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level);
        long generation = runtime.serverRuntime().definitions().orElseThrow().generation();
        for (var entry : List.copyOf(state.mirrors.entrySet())) {
            var entity = entry.getKey(); var mirror = entry.getValue();
            mirror.viewers.keySet().removeIf(player -> player.hasDisconnected() || player.level() != level || !level.players().contains(player));
            if (entity.isRemoved() || entity.level() != level || mirror.viewers.isEmpty()) { state.mirrors.remove(entity); continue; }
            String json;
            try { json = NativeEntityCombatMirror.capture(runtime, entity).toString(); }
            catch (RuntimeException failure) { json = io.github.fatalitaya.combatnexus.presentation.api.CombatPanelProjector.refused("CAPTURE_FAILED").toString(); }
            if (json.length() > CombatPresentationPayload.MAX_JSON) json = io.github.fatalitaya.combatnexus.presentation.api.CombatPanelProjector.refused("BUDGET_EXCEEDED").toString();
            if (!json.equals(mirror.json) || generation != mirror.generation) {
                mirror.json = json; mirror.generation = generation; mirror.revision = ++state.revision;
            }
            if (mirror.viewers.values().stream().allMatch(sent -> sent == mirror.revision)) continue;
            var payload = state.payload(entity, generation, mirror.revision, false, mirror.json);
            if (!payload.fitsEncodingBudget(level.registryAccess())) payload = state.payload(entity, generation, mirror.revision, false,
                io.github.fatalitaya.combatnexus.presentation.api.CombatPanelProjector.refused("ENCODING_FAILED").toString());
            for (var viewer : mirror.viewers.entrySet()) if (viewer.getValue() != mirror.revision) {
                PacketDistributor.sendToPlayer(viewer.getKey(), payload); viewer.setValue(mirror.revision);
            }
        }
    }
    private CombatPresentationPayload payload(LivingEntity entity, long generation, long revision, boolean removed, String json) {
        var level = (ServerLevel)entity.level();
        var session = ((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding)level).combatnexus$worldSession().id();
        return new CombatPresentationPayload(session, NativeIds.nexus(level.dimension().identifier()), generation,
            entity.getId(), entity.getUUID(), ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance)entity).combatnexus$incarnation(), revision, removed, json);
    }
    public static void clear(ServerPlayer player, ServerLevel level) { existing(level).ifPresent(state -> state.invalidateEntity(player.getUUID())); }
    public static void clear(ServerLevel level) { existing(level).ifPresent(CombatPresentationNetwork::reset); }
    private static Optional<CombatPresentationNetwork> existing(ServerLevel level) {
        var binding = (io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding)level;
        return io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(binding.combatnexus$serverBinding())
            .flatMap(server -> server.world(binding.combatnexus$worldSession())).map(world -> world.presentationNetwork());
    }
}
