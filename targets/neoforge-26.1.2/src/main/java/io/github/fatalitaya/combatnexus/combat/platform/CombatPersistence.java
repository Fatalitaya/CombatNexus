package io.github.fatalitaya.combatnexus.combat.platform;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import io.github.fatalitaya.combatnexus.effect.api.EffectService;

/** Entity-native save data; queued joins restore only after native UUID lookup becomes available. */
public final class CombatPersistence {
    private static final String KEY = "combatnexus:combat_state";
    private static final Map<ServerLevel, Set<UUID>> PENDING = new IdentityHashMap<>(), ACTIVE = new IdentityHashMap<>();
    private CombatPersistence() {}
    public static void joined(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) PENDING.computeIfAbsent(level, ignored -> new LinkedHashSet<>()).add(entity.getUUID());
    }
    public static void tick(ServerLevel level) {
        var pending = PENDING.remove(level); if (pending == null) return;
        for (var id : pending) {
            if (!(level.getEntity(id) instanceof LivingEntity entity) || !entity.isAlive()) continue;
            if (!ACTIVE.computeIfAbsent(level, ignored -> new HashSet<>()).add(id)) continue;
            var saved = entity.getPersistentData().getString(KEY);
            if (saved.isEmpty()) continue;
            try { CombatWorldLifecycle.access().restore(level, id, saved.orElseThrow()); }
            catch (RuntimeException failure) {
                // Failed restore must not make a subsequent save overwrite the original data.
                ACTIVE.get(level).remove(id);
                try { CombatWorldLifecycle.access().clear(level, id, EffectService.Removal.WORLD_EXIT); }
                catch (RuntimeException cleanupFailure) {
                }
            }
        }
    }
    public static void save(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) save(entity, level);
    }
    private static void save(LivingEntity entity, ServerLevel level) {
        if (!level.getServer().isSameThread()
                || !ACTIVE.getOrDefault(level, Set.of()).contains(entity.getUUID())) return;
        // CHANGED_DIMENSION / UNLOADED already mark an otherwise living entity removed before this event.
        if (entity.isDeadOrDying()) return;
        entity.getPersistentData().putString(KEY, CombatWorldLifecycle.access().save(level, entity.getUUID()));
    }
    public static void leaving(LivingEntity entity, EffectService.Removal reason) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        leaving(entity, level, reason);
    }
    public static void leaving(LivingEntity entity, ServerLevel level, EffectService.Removal reason) {
        if(ACTIVE.getOrDefault(level,Set.of()).contains(entity.getUUID())){
            var runtime=io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level);
            boolean death=reason==EffectService.Removal.DEATH||reason==EffectService.Removal.RESPAWN;
            runtime.serverRuntime().lifecycle(runtime,()->runtime.abilityControl().prepareLeave(entity.getUUID(),death));
            if(death){
                var state=new io.github.fatalitaya.combatnexus.combat.internal.persistence.CombatPersistentState(entity.getUUID(),level.getGameTime(),Set.of(),
                    List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),
                    new io.github.fatalitaya.combatnexus.combat.time.internal.SavedTime(0,0,null,List.of(),List.of()),
                    List.of(),runtime.abilityControl().saveCooldowns(entity.getUUID()),Map.of());
                entity.getPersistentData().putString(KEY,io.github.fatalitaya.combatnexus.combat.internal.persistence.CombatStateCodec.encode(state));
            }else save(entity,level);
        }
        var active = ACTIVE.get(level); if (active != null) active.remove(entity.getUUID());
        var pending = PENDING.get(level); if (pending != null) pending.remove(entity.getUUID());
    }
    public static void copyTransfer(LivingEntity original, LivingEntity replacement) {
        original.getPersistentData().getString(KEY).ifPresent(saved -> replacement.getPersistentData().putString(KEY, saved));
    }
    public static void close(ServerLevel level) {
        for (var id : List.copyOf(ACTIVE.getOrDefault(level, Set.of())))
            if (level.getEntity(id) instanceof LivingEntity entity){
                var runtime=io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level);
                runtime.serverRuntime().lifecycle(runtime,()->runtime.abilityControl().prepareLeave(id,false));
                save(entity);
            }
        ACTIVE.remove(level); PENDING.remove(level);
    }
}
