package io.github.fatalitaya.combatnexus.combat.regeneration.internal;
import io.github.fatalitaya.combatnexus.combat.regeneration.spi.RegenerationConditions;

import io.github.fatalitaya.combatnexus.combat.regeneration.api.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Custom recovery only, server-thread confined. Missed ticks never generate catch-up healing. */
public final class DefaultRegenerationScheduler implements RegenerationSchedulerControl {
    private static final System.Logger LOGGER = System.getLogger(DefaultRegenerationScheduler.class.getName());
    public RegenerationScheduler businessView() {
        return new RegenerationScheduler() {
            public void replace(RegenerationPlan plan) { DefaultRegenerationScheduler.this.replace(plan); }
            public void remove(UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId source) { DefaultRegenerationScheduler.this.remove(target, source); }
        };
    }
    public java.util.List<RegenerationPlan> snapshot(UUID target) { requireOpen(); return state.snapshot(target); }
    private final RegenerationScheduleState state;
    private final io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time;
    private final RegenerationConditions conditions;

    public void suspend(UUID target,NexusId source,boolean suspended){requireOpen();io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();state.suspend(target,source,suspended);}
    public void refresh(UUID target,NexusId source,RegenerationDefinition definition){requireOpen();io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();state.refresh(target,source,definition);}
    @Override public void recordDamage(UUID target, long gameTick) {
        // Internal settlement bookkeeping is valid even when the completed hit forbids child gameplay.
        // CombatRuntime verifies the current settlement callback and world before invoking this control.
        requireOpen();
        state.recordDamage(target, gameTick);
    }
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    private void requireOpen() {
        if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Recovery session closed or wrong thread");
    }
    @Override public void close() { requireOpen(); reset(); closed = true; }

    public DefaultRegenerationScheduler(RegenerationConditions conditions, io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) {
        this.time = Objects.requireNonNull(time);
        this.state = new RegenerationScheduleState(time);
        this.conditions = Objects.requireNonNull(conditions, "conditions");
    }

    @Override public void replace(RegenerationPlan plan) {
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.REGENERATION);
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(); requireOpen();
        state.replace(plan);
    }

    @Override public void tick(long gameTick, java.util.function.Consumer<io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest> healing) { requireOpen();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        Objects.requireNonNull(healing, "healing");
        var ready = state.advance(gameTick);
        for (var entry : ready) {
            var pending = entry.getValue();
            var plan = pending.plan;
            long now = time.now(plan.target(), plan.source().id());
            if (!state.current(entry.getKey(), pending)) continue;
            if (!time.mayExecute()) { state.defer(entry.getKey(),pending); continue; }
            if (plan.expiresAtTick().isPresent() && now >= plan.expiresAtTick().getAsLong()) {
                remove(plan.target(), plan.source().id()); continue;
            }
            String stage = "conditions";
            try {
                var observation = conditions.read(plan.target());
                stage = "accrual";
                double amount = state.accrue(pending, now, observation);
                if (amount > 0) {
                    stage = "healing";
                    healing.accept(new HealingRequest(UUID.randomUUID(), plan.target(),
                            plan.source(), HealingRequest.Reason.NATURAL_REGENERATION, amount));
                }
            } catch (RuntimeException failure) {
                LOGGER.log(System.Logger.Level.DEBUG, "Recovery source " + plan.source().id() + " failed during " + stage, failure);
            }
        }
    }

    public List<SavedRecovery> save(UUID target, Set<NexusId> sources, long now) {
        requireOpen();
        return state.save(target, sources, now).stream()
                .map(value -> new SavedRecovery(value.plan(), value.accrued(), value.ticks(), value.damageAge(),value.remainingDelay())).toList();
    }
    public void restore(UUID target, List<SavedRecovery> saved, Set<NexusId> sources) {
        requireOpen();
        for (var value : saved) {
            var plan = value.plan();
            if (!sources.contains(plan.source().id())) continue;
            long now=time.now(target,plan.source().id());
            long remaining = plan.expiresAtTick().isPresent() ? plan.expiresAtTick().getAsLong() - now : -1;
            if (plan.expiresAtTick().isPresent() && remaining <= 0) continue;
            var source = new io.github.fatalitaya.combatnexus.combat.api.CombatSource(plan.source().id(), plan.source().owner(), Optional.empty());
            var restored = new RegenerationPlan(target, source, plan.definition(), remaining < 0 ? OptionalLong.empty() : OptionalLong.of(Math.addExact(now, remaining)));
            replace(restored);
            state.restoreProgress(restored, value.accrued(), value.ticks(), value.damageAge(), now,value.remainingDelay());
        }
    }
    @Override public void remove(UUID target, NexusId source) {
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.REGENERATION);
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(); requireOpen(); state.remove(target, source); }
    @Override public void clear(UUID target) {
        io.github.fatalitaya.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle(); requireOpen(); state.clear(target); }
    @Override public void reset() {
        io.github.fatalitaya.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle(); requireOpen(); state.reset(); }
}
