package io.github.fatalitaya.combatnexus.combat.regeneration.internal;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.*;
import io.github.fatalitaya.combatnexus.combat.regeneration.spi.RegenerationConditions;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Internal schedule and progress state; the world adapter owns guards and healing callbacks. */
final class RegenerationScheduleState {
    record Key(UUID target, NexusId source) {}
    private final io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time;
    RegenerationScheduleState(io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) { this.time = Objects.requireNonNull(time); }
    static final class Pending {
        RegenerationPlan plan;
        boolean suspended;
        double accrued;
        int ticks;
        long task;
        long deadline;
        final long order;
        Pending(RegenerationPlan plan, long order) { this.plan = plan; this.order = order; }
        void reset() { accrued = 0; ticks = 0; }
    }
    private final Map<Key, Pending> plans = new LinkedHashMap<>();
    private final io.github.fatalitaya.combatnexus.combat.api.scheduling.DueQueue<Key> due = new io.github.fatalitaya.combatnexus.combat.api.scheduling.DueQueue<>(4096);
    private long sequence;
    private final Map<Key, Long> lastDamage = new HashMap<>();
    private final Map<UUID, Long> entityDamage = new HashMap<>();
    private static final NexusId COMBAT_CLOCK = NexusId.parse("combatnexus:recovery_combat_window");
    private static final long COMBAT_WINDOW_TICKS = 100;

    private long lastTick = Long.MIN_VALUE;
    List<RegenerationPlan> snapshot(UUID target) {
        return plans.values().stream().filter(p -> p.plan.target().equals(target)).map(p -> p.plan).toList();
    }
    void recordDamage(UUID target, long gameTick) {
        if (gameTick < 0) throw new IllegalArgumentException("Negative damage tick");
        Objects.requireNonNull(target);
        entityDamage.put(target,time.now(target,COMBAT_CLOCK));
        for(var key:plans.keySet()) if(key.target().equals(target)) lastDamage.put(key,time.now(target,key.source()));
    }
    void replace(RegenerationPlan plan) {
        Key key = new Key(plan.target(), plan.source().id());
        Pending previous = plans.get(key);
        if (previous == null || !previous.plan.equals(plan)) {
            if (previous == null && plans.size() >= 4096) throw new IllegalStateException("Recovery plan capacity exceeded");
            if (previous != null) due.cancel(previous.task);
            var pending = new Pending(plan, previous == null ? ++sequence : previous.order);
            pending.deadline = lastTick == Long.MIN_VALUE ? 0 : Math.addExact(time.now(key.target(), key.source()), 1);
            pending.task = due.schedule(pending.deadline,key);
            plans.put(key, pending);
            if(previous==null && !lastDamage.containsKey(key) && entityDamage.containsKey(key.target())) {
                long age=Math.max(0,time.now(key.target(),COMBAT_CLOCK)-entityDamage.get(key.target()));
                lastDamage.put(key,time.now(key.target(),key.source())-age);
            }
        }
    }

    List<Map.Entry<Key, Pending>> advance(long gameTick) {
        if (gameTick < 0 || lastTick != Long.MIN_VALUE && gameTick < lastTick) {
            throw new IllegalArgumentException("Clock moved backwards; reset the world session first");
        }
        if (gameTick == lastTick) return List.of();
        lastTick = gameTick;
        var ready = due.poll(key -> time.advancing(key.target(), key.source()) ? time.now(key.target(), key.source()) : Long.MIN_VALUE).stream().map(task -> Map.entry(task.value(), plans.get(task.value())))
                .sorted(Comparator.comparingLong(entry -> entry.getValue().order)).toList();
        // Reserve all next opportunities before any healing callback can throw or replace a source.
        for (var entry : ready) {
            entry.getValue().deadline=Math.addExact(time.now(entry.getKey().target(),entry.getKey().source()),1);
            entry.getValue().task=due.schedule(entry.getValue().deadline,entry.getKey());
        }
        return ready;
    }
    void defer(Key key, Pending pending) {
        if (!current(key,pending)) return;
        due.cancel(pending.task);pending.deadline=time.now(key.target(),key.source());pending.task=due.schedule(pending.deadline,key);
    }
    boolean current(Key key, Pending pending) { return plans.get(key) == pending; }
    double accrue(Pending pending, long gameTick, RegenerationConditions.State state) {
        if(pending.suspended)return 0;
        var plan = pending.plan;
        Long damage = lastDamage.get(new Key(plan.target(),plan.source().id()));

        boolean inCombat = damage != null && (gameTick < damage || gameTick - damage < COMBAT_WINDOW_TICKS);
        if (!state.alive()
                || plan.definition().respectsNaturalRegenerationRule() && !state.naturalRegeneration()
                || plan.definition().requiresOutOfCombat() && inCombat) {
            pending.reset();
            return 0;
        }
        if (damage != null) {
            long damageTick = damage;
            if (gameTick < damageTick || gameTick - damageTick < plan.definition().damageDelayTicks()) {
                pending.reset();
                return 0;
            }
        }
        // Aggregate 20 eligible ticks so fractional HP/s is not truncated every tick at a float health write.
        pending.accrued = io.github.fatalitaya.combatnexus.formula.Amounts.nonNegative(
                pending.accrued + plan.definition().healthPerSecond() / 20.0, "accrued recovery");
        if (++pending.ticks == 20) {
            double amount = pending.accrued;
            pending.reset(); // Consume before callback; cancellation/full health never accumulates a retry burst.
            return amount;
        }
        return 0;
    }
    record Saved(RegenerationPlan plan, double accrued, int ticks, Long damageAge, long remainingDelay) {}
    List<Saved> save(UUID target, Set<NexusId> sources, long now) {
        return plans.entrySet().stream().filter(e -> e.getKey().target().equals(target) && sources.contains(e.getKey().source()))
                .map(e -> new Saved(e.getValue().plan, e.getValue().accrued, e.getValue().ticks,
                        lastDamage.containsKey(e.getKey()) ? Math.max(0, time.now(target,e.getKey().source()) - lastDamage.get(e.getKey())) : null,Math.max(0,e.getValue().deadline-time.now(target,e.getKey().source())))).toList();
    }
    void restoreProgress(RegenerationPlan plan, double accrued, int ticks, Long damageAge, long now, long remainingDelay) {
        var pending = plans.get(new Key(plan.target(), plan.source().id()));
        pending.accrued = accrued; pending.ticks = ticks;
        due.cancel(pending.task);pending.deadline=Math.addExact(now,remainingDelay);
        pending.task=due.schedule(pending.deadline,new Key(plan.target(),plan.source().id()));
        if (damageAge != null) lastDamage.put(new Key(plan.target(),plan.source().id()), now - damageAge);
    }
    void suspend(UUID target,NexusId source,boolean suspended){var pending=plans.get(new Key(target,source));if(pending!=null)pending.suspended=suspended;}
    void refresh(UUID target,NexusId source,RegenerationDefinition definition){var pending=plans.get(new Key(target,source));if(pending!=null)pending.plan=new RegenerationPlan(target,pending.plan.source(),definition,pending.plan.expiresAtTick());}
    void remove(UUID target, NexusId source) {
        var previous = plans.remove(new Key(target, source)); if (previous != null) due.cancel(previous.task);
    }
    void clear(UUID target) {
        plans.entrySet().removeIf(entry -> { if (!entry.getKey().target().equals(target)) return false; due.cancel(entry.getValue().task); return true; }); lastDamage.keySet().removeIf(key->key.target().equals(target)); entityDamage.remove(target);
    }
    void reset() { plans.clear(); due.clear(); lastDamage.clear(); entityDamage.clear(); lastTick = Long.MIN_VALUE; }
}
