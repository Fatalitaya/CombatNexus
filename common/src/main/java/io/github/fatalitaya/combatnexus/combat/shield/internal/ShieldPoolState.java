package io.github.fatalitaya.combatnexus.combat.shield.internal;

import io.github.fatalitaya.combatnexus.combat.shield.api.*;
import io.github.fatalitaya.combatnexus.formula.Amounts;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Internal pool arithmetic and scheduling. No world, transaction or publication access. */
final class ShieldPoolState {
    record Change(UUID target, ShieldPoolSnapshot pool, ShieldPoolSnapshot.RemovalReason reason) {}
    record Key(UUID target, NexusId source) {}
    private final io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time;
    ShieldPoolState(io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) { this.time = Objects.requireNonNull(time); }
    private long now(Key key) { return time.now(key.target(), key.source()); }
    record DamageKey(UUID target, long damageId) {}
    record Allocation(Pool pool, double amount, Map<NexusId, Double> channels) {}
    private static final int MAX_RESERVATIONS_PER_TICK = 4096;
    private final Map<UUID, Map<Key, Pool>> pools = new LinkedHashMap<>();
    private final Set<Key> insertionOrder = new LinkedHashSet<>();
    private Map<Key, Pool> group(UUID target) { return pools.getOrDefault(target, Map.of()); }
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.DeadlineIndex<Key> expiry = new io.github.fatalitaya.combatnexus.combat.internal.coordination.DeadlineIndex<>();
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.DeadlineIndex<Key> waiting = new io.github.fatalitaya.combatnexus.combat.internal.coordination.DeadlineIndex<>();
    private final Set<Key> recovering = new LinkedHashSet<>();
    private final Map<Key, Long> order = new HashMap<>();
    private long sequence;
    private final Map<UUID, List<Pool>> orderedPools = new HashMap<>();
    private void scheduleRecovery(Key key, Pool pool) {
        waiting.cancel(key); recovering.remove(key);
        var definition = pool.grant.definition();
        if (pool.suspended || pool.balance >= definition.capacity() || definition.recoveryPerSecond() <= 0) return;
        long delay = definition.recoveryDelayTicks();
        long due = pool.damaged ? (pool.lastLoss > Long.MAX_VALUE - delay ? Long.MAX_VALUE : pool.lastLoss + delay) : now(key);
        if (due <= now(key)) recovering.add(key); else waiting.schedule(key, due);
    }
    private void installed(Key key, Pool pool) {
        orderedPools.remove(key.target()); order.computeIfAbsent(key, ignored -> sequence++);
        expiry.cancel(key); pool.grant.expiresAtTick().ifPresent(deadline -> expiry.schedule(key, deadline));
        scheduleRecovery(key, pool);
    }
    private void unschedule(Key key) {
        expiry.cancel(key); waiting.cancel(key); recovering.remove(key); order.remove(key); orderedPools.remove(key.target());
    }
    private final Map<DamageKey, AllocationState> reservations = new HashMap<>();
    private final List<Change> changes = new ArrayList<>();
    private long tick;
    private boolean started;

    final class Pool {
        final ShieldGrant grant;
        final Set<NexusId> eligibleDamageTypes;
        double balance;
        double held;
        long lastLoss;
        boolean damaged;
        boolean suspended;
        long lastRecovery = -1;
        Pool(ShieldGrant grant, Set<NexusId> eligible) {
            this.grant = grant; balance = grant.initialBalance();
            eligibleDamageTypes = Set.copyOf(eligible);
        }
        ShieldPoolSnapshot snapshot() {
            return new ShieldPoolSnapshot(grant.source(), grant.definition(), balance, grant.expiresAtTick(), eligibleDamageTypes);
        }
    }

    public record Saved(ShieldGrant grant, double balance, long damageAge, boolean damaged) {
        public Saved {
            Objects.requireNonNull(grant); Amounts.nonNegative(balance, "saved balance");
            if (balance > grant.definition().capacity() || damageAge < 0) throw new IllegalArgumentException("Invalid saved shield state");
        }
    }
    public List<Saved> save(UUID target, Set<NexusId> sources) {
        requireNoPending(target);
        return ordered(target).stream().filter(p -> sources.contains(p.grant.source().id()))
                .map(p -> new Saved(p.grant, p.balance, Math.max(0, time.now(target, p.grant.source().id()) - p.lastLoss), p.damaged)).toList();
    }
    public List<ShieldPoolSnapshot> snapshot(UUID target, java.util.function.IntConsumer budget) {
        var result = new ArrayList<ShieldPoolSnapshot>();
        for (var pool : ordered(target)) {
            budget.accept(1); result.add(pool.snapshot());
        }
        return List.copyOf(result);
    }

    private List<Pool> ordered(UUID target) {
        Objects.requireNonNull(target, "target");
        return orderedPools.computeIfAbsent(target, ignored -> group(target).entrySet().stream()
                .map(Map.Entry::getValue).sorted(Comparator
                        .comparingInt((Pool p) -> p.grant.definition().priority()).reversed()
                        .thenComparing(p -> p.grant.source().id().toString())).toList());
    }

    void checkGrant(UUID target, ShieldGrant grant) {
        requireNoPending(target);
        if (grant.expiresAtTick().isPresent() && grant.expiresAtTick().getAsLong() <= time.now(target, grant.source().id()))
            throw new IllegalArgumentException("Cannot install an expired shield");
    }
    Pool prepare(UUID target, ShieldGrant grant, Set<NexusId> eligible) {
        var definition = grant.definition();
        Key key = new Key(Objects.requireNonNull(target, "target"), grant.source().id());
        Pool previous = group(target).get(key);
        Pool replacement = new Pool(grant, eligible);
        if (previous != null && definition.stacking() == ShieldDefinition.Stacking.REFRESH_SAME_SOURCE) {
            replacement.balance = Math.min(previous.balance, definition.capacity());
            replacement.lastLoss = previous.lastLoss;
            replacement.damaged = previous.damaged;
        }
        return replacement;
    }
    double install(UUID target, Pool replacement) {
        var key = new Key(target, replacement.grant.source().id());
        var previous = group(target).get(key);
        replacement.lastRecovery = now(key);
        pools.computeIfAbsent(target, ignored -> new LinkedHashMap<>()).put(key, replacement);
        insertionOrder.add(key); installed(key, replacement);
        return Math.max(0, replacement.balance - (previous == null ? 0 : previous.balance));
    }
    void suspend(UUID target,NexusId source,boolean suspended){
        requireNoPending(target);var key=new Key(target,source);var pool=group(target).get(key);
        if(pool!=null){pool.suspended=suspended;scheduleRecovery(key,pool);}
    }
    void refresh(UUID target,NexusId source,ShieldDefinition definition,Set<NexusId> eligible){
        requireNoPending(target);var key=new Key(target,source);var old=group(target).get(key);if(old==null)return;
        var next=new Pool(new ShieldGrant(old.grant.source(),definition,Math.min(old.balance,definition.capacity()),old.grant.expiresAtTick()),eligible);
        next.damaged=old.damaged;next.lastLoss=old.lastLoss;next.suspended=old.suspended;install(target,next);
    }
    void restore(UUID target, ShieldGrant grant, Set<NexusId> eligible, boolean damaged, long lastLoss) {
        var pool = new Pool(grant, eligible); pool.damaged = damaged; pool.lastLoss = lastLoss;
        install(target, pool);
    }
    void remove(UUID target, NexusId source, ShieldPoolSnapshot.RemovalReason reason) {
        var group = pools.get(target);
        Pool removed = group == null ? null : group.remove(new Key(target, source));
        insertionOrder.remove(new Key(target, source));
        unschedule(new Key(target, source));
        if (group != null && group.isEmpty()) pools.remove(target);
        if (removed != null) changes.add(new Change(target, removed.snapshot(), reason));
    }

    static LinkedHashMap<NexusId, Double> channels(double amount, Map<NexusId, Float> channels) {
        var channelRemainder = new LinkedHashMap<NexusId, Double>();
        double total = 0;
        for (var entry : channels.entrySet()) {
            Objects.requireNonNull(entry.getKey()); Amounts.nonNegative(entry.getValue(), "channel damage");
            total += entry.getValue();
        }
        if (total > 0) {
            for (var entry : channels.entrySet()) channelRemainder.put(entry.getKey(), amount * entry.getValue() / total);
        } else channelRemainder.put(NexusId.fromNamespaceAndPath("combatnexus", "untyped"), amount);
        return channelRemainder;
    }
    void checkReservation(UUID target, long damageId, boolean enlisted) {
        if (reservations.containsKey(new DamageKey(target, damageId))) throw new IllegalStateException("Damage already reserved this tick");
        if (!enlisted && reservations.size() >= MAX_RESERVATIONS_PER_TICK) throw new IllegalStateException("Reservation budget exceeded");
    }
    AllocationState reserve(UUID target, long damageId, NexusId damageType, double amount, Map<NexusId, Double> channelRemainder) {
        return reserve(target, damageId, damageType, amount, channelRemainder, amount, Set.of());
    }
    AllocationState reserve(UUID target, long damageId, NexusId damageType, double amount, Map<NexusId, Double> channelRemainder,
            double maximumAbsorption, Set<NexusId> existingSources) {
        List<Allocation> allocations = new ArrayList<>();
        double remaining = amount;
        var pools = ordered(target).stream().sorted(Comparator.comparing(p -> !existingSources.contains(p.grant.source().id()))).toList();
        for (Pool pool : pools) {
            if (remaining == 0) break;
            if(pool.suspended)continue;
            if (!pool.grant.definition().unrestricted() && !pool.eligibleDamageTypes.contains(damageType)) continue;
            var selected = pool.grant.definition().channels();
            double eligible = channelRemainder.entrySet().stream()
                    .filter(e -> selected.isEmpty() || selected.contains(e.getKey())).mapToDouble(Map.Entry::getValue).sum();
            double take = Math.min(Math.max(0, maximumAbsorption - (amount - remaining)), Math.min(remaining, Math.min(eligible, Math.max(0, pool.balance - pool.held))));
            if (take == 0) continue;
            pool.held += take;
            var absorbedChannels = new LinkedHashMap<NexusId, Double>();
            for (var entry : channelRemainder.entrySet()) {
                if (!selected.isEmpty() && !selected.contains(entry.getKey())) continue;
                double absorbed = take * (entry.getValue() / eligible);
                absorbedChannels.put(entry.getKey(), absorbed);
                entry.setValue(Math.max(0, entry.getValue() - absorbed));
            }
            allocations.add(new Allocation(pool, take, Map.copyOf(absorbedChannels)));
            remaining = Math.max(0, remaining - take);
        }
        var allocation = new AllocationState(target, damageId, allocations, amount - remaining, remaining);
        return allocation;
    }

    void remember(AllocationState allocation) {
        reservations.put(new DamageKey(allocation.target, allocation.damageId), allocation);
    }

    final class AllocationState {
        final UUID target;
        final long damageId;
        final List<Allocation> allocations;
        final double absorbed;
        final double remaining;
        private boolean pending = true;
        AllocationState(UUID target, long damageId, List<Allocation> allocations, double absorbed, double remaining) {
            this.target = target; this.damageId = damageId; this.allocations = List.copyOf(allocations);
            this.absorbed = absorbed; this.remaining = remaining;
        }
        boolean pending() { return pending; }
        void release() {
            if (!pending) return;
            pending = false;
            for (var allocation : allocations) {
                var pool = allocation.pool(); pool.held = Math.max(0, pool.held - allocation.amount());
            }
        }
        ShieldCommitReceipt commit() {
            if (!pending) throw new IllegalStateException("Reservation released or expired");
            pending = false;
            var writes = new ArrayList<io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry>();
            for (Allocation allocation : allocations) {
                Pool pool = allocation.pool();
                double before = pool.balance;
                pool.held = Math.max(0, pool.held - allocation.amount());
                pool.balance = Math.max(0, before - allocation.amount());
                writes.add(io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry.write(
                        io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Resource.SHIELD, pool.grant.source().id(), before, pool.balance, before - pool.balance, true));
                pool.lastLoss = time.now(target, pool.grant.source().id());
                pool.damaged = true;
                scheduleRecovery(new Key(target, pool.grant.source().id()), pool);
                if (before > 0 && pool.balance == 0) {
                    changes.add(new Change(target, pool.snapshot(), ShieldPoolSnapshot.RemovalReason.BROKEN));
                }
            }
            var absorbedChannels = new LinkedHashMap<NexusId, Double>();
            allocations.forEach(a -> a.channels().forEach((channel, value) -> absorbedChannels.merge(channel, value, Double::sum)));
            return new ShieldCommitReceipt(target, damageId, absorbed, writes, absorbedChannels);
        }
    }
    boolean advanceRequired(long gameTick) {
        if (gameTick < 0 || started && gameTick < tick) throw new IllegalArgumentException("Invalid world clock");
        return !started || gameTick != tick;
    }
    void clearReservations() { reservations.values().forEach(AllocationState::release); reservations.clear(); }
    void tick(long gameTick) {
        boolean recover = started;
        started = true;
        tick = gameTick;
        var expired = expiry.due(key -> time.advancing(key.target(),key.source()) ? now(key) : Long.MIN_VALUE);
        expired.sort(Comparator.comparingLong(order::get));
        for (var key : expired) {
            var group = pools.get(key.target()); var pool = group.remove(key);
            changes.add(new Change(key.target(), pool.snapshot(), ShieldPoolSnapshot.RemovalReason.EXPIRED));
            if (group.isEmpty()) pools.remove(key.target());
            insertionOrder.remove(key); unschedule(key);
        }
        for (var key : waiting.due(key -> time.advancing(key.target(),key.source()) ? now(key) : Long.MIN_VALUE)) { waiting.cancel(key); recovering.add(key); }
        if (recover) for (var key : List.copyOf(recovering)) {
            if (!time.advancing(key.target(),key.source())) continue;
            var pool = group(key.target()).get(key); var definition = pool.grant.definition();
            long current = now(key);
            if (pool.lastRecovery == current) continue;
            pool.lastRecovery = current;
            // Exactly one increment for an actual tick, including after a clock jump.
            pool.balance += Math.min(definition.capacity() - pool.balance, definition.recoveryPerSecond() / 20.0);
            if (pool.balance >= definition.capacity()) recovering.remove(key);
        }
    }

    void requireNoPending(UUID target) {
        if (reservations.entrySet().stream().anyMatch(e -> e.getKey().target().equals(target) && e.getValue().pending)) {
            throw new IllegalStateException("Cannot mutate pools during a damage reservation");
        }
    }
    void clear(UUID target) {
        reservations.forEach((key, allocation) -> { if (key.target().equals(target)) allocation.release(); });
        orderedPools.remove(target);
        var removed = pools.remove(target);
        if (removed != null) { insertionOrder.removeAll(removed.keySet()); removed.keySet().forEach(this::unschedule); }
        changes.removeIf(change -> change.target().equals(target));
    }
    void reset() {
        reservations.values().forEach(AllocationState::release); reservations.clear();
        pools.clear();
        insertionOrder.clear();
        expiry.clear(); waiting.clear(); recovering.clear(); order.clear(); orderedPools.clear();
        changes.clear();
        started = false;
    }
    boolean started() { return started; }
    public List<Change> drainChanges() {
        List<Change> result = List.copyOf(changes);
        changes.clear();
        return result;
    }
}
