package io.github.fatalitaya.combatnexus.combat.platform;
import io.github.fatalitaya.combatnexus.combat.api.platform.NativeHealingApplication;

import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.EventHooks;

/** Mirrors 26.1.2 heal: event first, then sample and write native health exactly once. */
public final class WorldHealingPort implements NativeHealingApplication {
    private final ServerLevel level;
    @FunctionalInterface public interface NativeHealthWriter { void write(LivingEntity target, float health); }
    private final NativeHealthWriter writer;
    public WorldHealingPort(ServerLevel level) { this(level, LivingEntity::setHealth); }
    public WorldHealingPort(ServerLevel level, NativeHealthWriter writer) {
        this.level = java.util.Objects.requireNonNull(level);
        this.writer = java.util.Objects.requireNonNull(writer);
    }
    @Override public Attempt open(HealingRequest request, long generation) {
        var entity = level.getEntity(request.target()) instanceof LivingEntity living && !living.isRemoved() ? living : null;
        var world = io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level);
        var session = world.worldSession();
        var observed = entity == null ? java.util.Optional.<HealingParticipant>empty() : java.util.Optional.of(
                new HealingParticipant(session, new io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity(
                        entity.getUUID(), ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance) entity).combatnexus$incarnation()),
                        entity.isAlive(), entity.isAlive() ? java.util.Optional.of(world.captureInput(entity, generation)) : java.util.Optional.empty()));
        return new BoundAttempt(entity, observed, session, request.target());
    }
    private final class BoundAttempt implements Attempt {
        private LivingEntity target;
        private final java.util.Optional<HealingParticipant> observed;
        private final io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession session;
        private final java.util.UUID targetId;
        private boolean closed;
        private BoundAttempt(LivingEntity target, java.util.Optional<HealingParticipant> observed,
                io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession session, java.util.UUID targetId) {
            this.target = target; this.observed = observed; this.session = session; this.targetId = targetId;
        }
        private void requireOpen() { if (closed) throw new IllegalStateException("Healing attempt closed"); }
        public java.util.Optional<HealingParticipant> target() { requireOpen(); return observed; }
        public Receipt apply(double amount) {
            requireOpen();
            if (((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) level).combatnexus$worldSession() != session
                    || target != null && level.getEntity(targetId) != target)
                return Receipt.skipped(State.REJECTED, 0);
            return applyExact(target, amount);
        }
        public void close() { closed = true; target = null; }
    }
    private Receipt applyExact(LivingEntity target, double amount) {
        if (target == null) return Receipt.skipped(State.REJECTED, 0);
        if (!target.isAlive()) return Receipt.skipped(State.DEAD_TARGET, 0);
        if (amount == 0) return Receipt.skipped(State.ZERO_AMOUNT, 0);
        float accepted = EventHooks.onLivingHeal(target, (float) Math.min(amount, Float.MAX_VALUE));
        if (!Float.isFinite(accepted) || accepted <= 0) return Receipt.skipped(State.CANCELLED, 0);
        if (!target.isAlive() || target.isRemoved() || target.level() != level) {
            return Receipt.skipped(State.DEAD_TARGET, accepted);
        }
        // Sampling after callbacks excludes nested heals performed by event subscribers.
        float before = target.getHealth();
        float maximum = target.getMaxHealth();
        float next = (float) Math.min(maximum, (double) before + accepted);
        var write = EquipmentCombatAdapter.nativeDamage().observeHealthWrite(target, () -> writer.write(target, next));
        return new Receipt(write.uncontended() ? State.WRITTEN : State.UNKNOWN_WRITE,
            accepted, before, maximum, target.getHealth(), write.completed() && write.uncontended());
    }
}
