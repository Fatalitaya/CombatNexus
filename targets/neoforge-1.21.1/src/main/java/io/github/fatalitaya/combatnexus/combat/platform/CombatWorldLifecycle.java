package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.combat.api.platform.WorldLifecycleAccess;
import io.github.fatalitaya.combatnexus.effect.api.EffectService;
import io.github.fatalitaya.combatnexus.presentation.platform.CombatPresentationNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.IdentityHashMap;

/** Owns world sessions and invalidates all retained service references at unload. */
@EventBusSubscriber(modid = "combatnexus")
public final class CombatWorldLifecycle {
    private static final java.util.Set<ServerLevel> WORLDS = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static WorldLifecycleAccess access;
    private CombatWorldLifecycle() {}
    public static WorldLifecycleAccess access() { return java.util.Objects.requireNonNull(access, "World adapter not registered"); }
    @SubscribeEvent public static void platform(io.github.fatalitaya.combatnexus.combat.platform.api.CombatRegisterEvent event) {
        access = event.registrar().worldLifecycle();
    }
    @SubscribeEvent public static void register(io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent event) {
        event.registrar().registerSettlementListener(io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("combatnexus", "lifesteal"), 0, callback -> {
            access().damageSettled(callback);
        });
    }

    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer viewer
                && event.getTarget() instanceof LivingEntity target && target.level() instanceof ServerLevel level && WORLDS.contains(level))
            io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level).presentationNetwork().track(target, viewer);
    }
    @SubscribeEvent public static void untracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer viewer
                && event.getTarget() instanceof LivingEntity target && target.level() instanceof ServerLevel level && WORLDS.contains(level))
            io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level).presentationNetwork().untrack(target, viewer);
    }
    @SubscribeEvent public static void equipment(net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive() || !WORLDS.contains(level)) return;
        try {
            access().equipmentChanged(level, entity.getUUID(),
                    io.github.fatalitaya.combatnexus.platform.NativeEquipment.position(event.getSlot()), WorldEquipmentObservation.capture(level, event.getTo()));
        } catch (RuntimeException failure) {
        }
    }
    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living && event.getLevel() instanceof ServerLevel level && WORLDS.contains(level)) {
            CombatPersistence.joined(living);
            access().trackEquipment(level, living.getUUID());
        }
    }
    @SubscribeEvent public static void load(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (WORLDS.contains(level)) return;
        var kernel = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of(
                (io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) level.getServer());
        ((NativeWorldRuntimeBinding) level).combatnexus$bindWorldSession(
                new io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession(kernel.session(),
                        io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(level.dimension().location())));
        access().load(level, new io.github.fatalitaya.combatnexus.combat.api.platform.NativeWorldHooks() {
            public io.github.fatalitaya.combatnexus.combat.api.platform.ParticipantStateAccess participantStates() { return new WorldParticipantStates(level); }
            public io.github.fatalitaya.combatnexus.combat.api.platform.DamageTypeCatalog damageTypes() { return new WorldDamageTypes(level); }
            public io.github.fatalitaya.combatnexus.combat.api.platform.NativeHealingApplication healing() { return new WorldHealingPort(level); }
            public io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileDamageWorldAccess projectileDamage() { return new io.github.fatalitaya.combatnexus.combat.projectile.platform.WorldProjectileDamageAccess(level); }
            public io.github.fatalitaya.combatnexus.combat.api.platform.HealthWorldAccess health() { return new WorldHealthAccess(level); }
            public io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionWorldAccess absorption() { return new WorldAbsorptionAccess(level); }
            public io.github.fatalitaya.combatnexus.combat.api.platform.EntityMotionAccess motion() { return new WorldEntityMotionAccess(level); }
            public io.github.fatalitaya.combatnexus.combat.api.platform.EquipmentObservation equipment() { return new WorldEquipmentObservation(level); }
            public io.github.fatalitaya.combatnexus.attribute.api.platform.AttributeWorldAccess attributes() { return new io.github.fatalitaya.combatnexus.attribute.platform.NativeAttributeWorldAccess(level); }
            public boolean mayExpire(io.github.fatalitaya.combatnexus.effect.api.EffectInstance effect) {
                return io.github.fatalitaya.combatnexus.effect.platform.NativeEffectBridge.mayExpire(level, effect);
            }
            public void effectCommitted(EffectService.Change change) {
                io.github.fatalitaya.combatnexus.effect.platform.NativeEffectBridge.committed(level, change);
            }
        });
        WORLDS.add(level);
        for (var entity : level.getAllEntities()) if (entity instanceof LivingEntity living) {
            CombatPersistence.joined(living);
            access().trackEquipment(level, entity.getUUID());
        }
        access().beginTick(level);
    }
    /** Before scheduled commands can cause gameplay writes at the newly incremented time. */
    public static void clockAdvanced(ServerLevel level) {
        access().beginTick(level);
    }
    /** After native time advancement, before block/entity ticks; also runs for frozen dimensions. */
    public static void tickReady(ServerLevel level) {
            access().beginTick(level);
            CombatPersistence.tick(level);
            access().tickRecovery(level);
            CombatPresentationNetwork.flushControls(level);
            CombatPresentationNetwork.flushMirrors(level);
    }
    private static void clear(net.minecraft.world.entity.Entity entity, EffectService.Removal reason) {
        if (entity.level() instanceof ServerLevel level) clear(entity, reason, level);
    }
    private static void clear(net.minecraft.world.entity.Entity entity, EffectService.Removal reason, ServerLevel level) {
        if (entity instanceof LivingEntity living) CombatPersistence.leaving(living, level, reason);
        if (entity instanceof net.minecraft.server.level.ServerPlayer player) CombatPresentationNetwork.clear(player, level);
        access().clear(level, entity.getUUID(), reason);
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var reason = event.getEntity().getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION
                ? EffectService.Removal.DIMENSION_CHANGE : EffectService.Removal.WORLD_EXIT;
            clear(event.getEntity(), reason, level);
        }
    }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) { if (!event.isCanceled()) clear(event.getEntity(), EffectService.Removal.DEATH); }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            CombatPersistence.copyTransfer(event.getOriginal(), event.getEntity());
            return;
        }
        clear(event.getOriginal(), EffectService.Removal.RESPAWN);
        clear(event.getEntity(), EffectService.Removal.RESPAWN);
        CombatPersistence.copyTransfer(event.getOriginal(),event.getEntity());
        CombatPersistence.joined(event.getEntity());
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) CombatPresentationNetwork.clear(player, player.serverLevel());
        if (event.getEntity().level() instanceof ServerLevel level && WORLDS.contains(level)) {
            CombatPersistence.joined(event.getEntity());
            access().trackEquipment(level, event.getEntity().getUUID());
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity(), EffectService.Removal.WORLD_EXIT); }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var level : java.util.List.copyOf(WORLDS)) {
            if (level.getServer() == event.getServer()) {
                CombatPersistence.close(level);
                CombatPresentationNetwork.clear(level);
                WORLDS.remove(level);
                access().close(level);
                ((NativeWorldRuntimeBinding) level).combatnexus$bindWorldSession(null);
            }
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CombatPersistence.close(level);
            CombatPresentationNetwork.clear(level);
            WORLDS.remove(level);
            access().close(level);
                ((NativeWorldRuntimeBinding) level).combatnexus$bindWorldSession(null);
        }
    }
}
