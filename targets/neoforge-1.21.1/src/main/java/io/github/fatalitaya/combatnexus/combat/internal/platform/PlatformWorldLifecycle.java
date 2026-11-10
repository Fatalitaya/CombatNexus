package io.github.fatalitaya.combatnexus.combat.internal.platform;

import io.github.fatalitaya.combatnexus.combat.api.platform.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.spi.*;
import io.github.fatalitaya.combatnexus.combat.healing.spi.HealingApplicationPort;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingParticipant;
import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.effect.spi.EffectContributionPort;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.EquipmentItem;
import java.util.UUID;

/** Converts platform notifications to internal, finally-scoped lifecycle permits. */
public final class PlatformWorldLifecycle implements WorldLifecycleAccess {
    public static final WorldLifecycleAccess INSTANCE = new PlatformWorldLifecycle();
    private PlatformWorldLifecycle() {}
    private static void thread(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("World lifecycle requires the server thread");
    }
    private static void lifecycleInput(ServerLevel level) {
        thread(level);
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.requireWritable();
        if (io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.isCurrentPhase(
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.COMPUTE))
            throw new IllegalStateException("Native lifecycle notifications are forbidden during computation");
    }
    public static CombatRuntime existing(ServerLevel level) {
        thread(level);
        return CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer())).flatMap(server -> server.world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession()))
            .orElseThrow(() -> new IllegalStateException("World combat session is not loaded"));
    }
    public boolean loaded(ServerLevel level) {
        thread(level);
        return CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer())).flatMap(server -> server.world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession())).isPresent();
    }
    public void load(ServerLevel level, NativeWorldHooks hooks) {
        thread(level);
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        if (loaded(level)) return;
        var nativeHealing = java.util.Objects.requireNonNull(hooks.healing());
        var healing = healingApplication(nativeHealing);
        var attributes = new io.github.fatalitaya.combatnexus.attribute.internal.EffectAttributeContributions(hooks.attributes());
        var effects = new EffectContributionPort() {
            public Prepared prepare(EffectService.Change change) { return attributes.prepare(change); }
            public boolean mayExpire(EffectInstance effect) { return hooks.mayExpire(effect); }
            public void committed(EffectService.Change change) { hooks.effectCommitted(change); }
        };
        new CombatRuntime(CombatServerRuntime.of((ServerRuntimeBinding) level.getServer()),
                ((WorldRuntimeBinding) level).combatnexus$worldSession(), level::getGameTime, level, healing, id -> {
            var entity = level.getEntity(id);
            return new io.github.fatalitaya.combatnexus.combat.regeneration.spi.RegenerationConditions.State(
                entity instanceof LivingEntity living && living.isAlive(),
                level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION));
        }, effect -> false, effects, hooks.equipment(), hooks.motion(), hooks.absorption(), hooks.health(), hooks.projectileDamage(), hooks.damageTypes(), hooks.participantStates());
    }
    public static HealingApplicationPort healingApplication(NativeHealingApplication nativeHealing) {
        return new HealingApplicationPort() {
            public Attempt open(HealingRequest request, long generation) {
                var nativeAttempt = nativeHealing.open(request, generation);
                return new Attempt() {
                    public java.util.Optional<HealingParticipant> target() { return nativeAttempt.target(); }
                    public Receipt apply(double amount) {
                        var receipt = nativeAttempt.apply(amount);
                        return new Receipt(State.valueOf(receipt.state().name()), receipt.acceptedAmount(), receipt.healthBefore(),
                                receipt.maximumHealth(), receipt.healthAfter(), receipt.completed());
                    }
                    public void close() { nativeAttempt.close(); }
                };
            }
        };
    }
    public void beginTick(ServerLevel level) { lifecycleInput(level); CombatWorldLifecycleAccess.beginTick(existing(level)); }
    public void tickRecovery(ServerLevel level) { lifecycleInput(level); CombatWorldLifecycleAccess.tickRecovery(existing(level)); }
    public String save(ServerLevel level, UUID target) { lifecycleInput(level); return CombatWorldLifecycleAccess.save(existing(level), target); }
    public void restore(ServerLevel level, UUID target, String saved) { lifecycleInput(level); CombatWorldLifecycleAccess.restore(existing(level), target, saved); }
    public void clear(ServerLevel level, UUID target, EffectService.Removal reason) {
        lifecycleInput(level);
        if (loaded(level)) CombatWorldLifecycleAccess.clear(existing(level), target, reason);
    }
    public void close(ServerLevel level) {
        lifecycleInput(level);
        if (loaded(level)) CombatWorldLifecycleAccess.close(existing(level));
    }
    public void trackEquipment(ServerLevel level, UUID target) { CombatEquipmentAccess.track(existing(level), target); }
    public void equipmentChanged(ServerLevel level, UUID target, io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition slot, EquipmentItem stack) {
        CombatEquipmentAccess.changed(existing(level), target, slot, stack);
    }
    public boolean heal(LivingEntity target, float amount, io.github.fatalitaya.combatnexus.combat.healing.api.NativeHealingSource source) {
        if (!(target.level() instanceof ServerLevel level)) return false;
        thread(level);
        if (NativeHealingAccess.route() == NativeHealingAccess.Route.REJECTED_PHASE)
            throw new IllegalStateException("Native healing is forbidden during combat computation/submission");
        if (!Float.isFinite(amount) || amount <= 0) return true;
        if (!target.isAddedToLevel() && !target.isRemoved()) return false;
        NativeHealingAccess.submit(existing(level), new io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity(
                target.getUUID(), ((ParticipantInstance) target).combatnexus$incarnation()), amount, source);
        return true;
    }
    public void damageSettled(io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementCallback callback) {
        CombatBehaviorAccess.damage(existing(callback.context().level()), callback);
    }
    public void projectileLeft(ServerLevel level, net.minecraft.world.entity.Entity projectile) {
        lifecycleInput(level);
        if (projectile instanceof net.minecraft.world.entity.projectile.Projectile nativeProjectile)
            io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectilePersistence.saving(nativeProjectile);
        io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileFacts.launch(projectile).ifPresent(launch->{
            boolean finished=io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer())).flatMap(k->k.world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession())).map(w->w.decisionSamples().release(launch.action(),io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectilePersistence.owner(projectile))).orElse(false);
            if(finished && level.getEntity(launch.shooter().entity()) instanceof io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates states)
                states.combatnexus$states().clear(io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.ACTION_FINISHED,java.util.Optional.of(launch.action()));
        });
    }
    public void collision(ServerLevel level, net.minecraft.world.entity.projectile.Projectile projectile, net.minecraft.world.phys.HitResult hit, java.util.Optional<UUID> subject) {
        lifecycleInput(level);
        if (projectile.level() != level) throw new IllegalArgumentException("Projectile world mismatch");
        io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileFacts.launch(projectile).ifPresent(launch->{
            var direct=hit instanceof net.minecraft.world.phys.EntityHitResult entityHit?java.util.Optional.of(entityHit.getEntity().getUUID()):java.util.Optional.<java.util.UUID>empty();
            io.github.fatalitaya.combatnexus.combat.spi.CombatMomentAccess.publish(level,new io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.Collision(launch,new io.github.fatalitaya.combatnexus.api.geometry.Position3(hit.getLocation().x,hit.getLocation().y,hit.getLocation().z),direct,subject));
        });
    }
    public void cloneState(net.minecraft.server.level.ServerPlayer original, net.minecraft.server.level.ServerPlayer replacement, boolean death) {
        lifecycleInput(replacement.serverLevel());
        if (original.level().getServer() != replacement.level().getServer()) throw new IllegalArgumentException("Player server mismatch");
        var originalState=(io.github.fatalitaya.combatnexus.combat.state.spi.NativeCooldownTarget)original;
        var replacementState=(io.github.fatalitaya.combatnexus.combat.state.spi.NativeCooldownTarget)replacement;
        originalState.combatnexus$cooldowns().forEach((id, stamp) -> {
            if(!death || stamp.deathPolicy()==io.github.fatalitaya.combatnexus.combat.state.api.CooldownStamp.DeathPolicy.RETAIN)
                replacementState.combatnexus$cooldowns().put(id,stamp);
        });
        var states=((io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates)replacement).combatnexus$states();
        states.restore(((io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates)original).combatnexus$states().encode(false));
        states.clear(io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.RESPAWN,java.util.Optional.empty());
        if(death)states.clear(io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.DEATH,java.util.Optional.empty());
        var schedules=((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)replacement).combatnexus$schedules();
        schedules.restore(((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)original).combatnexus$schedules().encode(false));
        schedules.clear(io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.RESPAWN,java.util.Optional.empty());
        if(death)schedules.clear(io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.DEATH,java.util.Optional.empty());
    }
    public void changedDimension(net.minecraft.server.level.ServerPlayer player) {
        lifecycleInput(player.serverLevel());
        ((io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates)player).combatnexus$states().clear(io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.DIMENSION_CHANGE,java.util.Optional.empty());
        ((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)player).combatnexus$schedules().clear(io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.DIMENSION_CHANGE,java.util.Optional.empty());
    }

}
