package io.github.fatalitaya.combatnexus.combat.shield.internal;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatListeners;
import io.github.fatalitaya.combatnexus.combat.shield.spi.ShieldPoolService;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatCompletionQueue;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatPublicationGate;

import io.github.fatalitaya.combatnexus.combat.shield.api.*;
import io.github.fatalitaya.combatnexus.formula.Amounts;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;

/** Source pools consume explicit channel proportions without owning native absorption or events. */
public final class DefaultShieldService implements ShieldPoolServiceControl {
    public ShieldPoolService businessView() {
        return new ShieldPoolService() {
            public ShieldGrantReceipt replace(UUID target, ShieldGrant pool) { return DefaultShieldService.this.replace(target, pool); }
            public ShieldGrantReceipt replace(UUID target, ShieldGrant pool, io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent parent) { return DefaultShieldService.this.replace(target, pool, parent); }
            public Reservation reserve(UUID target, long damageId, NexusId damageType, double amount,
                        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent parent) { return DefaultShieldService.this.reserve(target, damageId, damageType, amount, parent); }
            public Reservation reserve(UUID target, long damageId, NexusId damageType, double amount, Map<NexusId, Float> channels) {
                return DefaultShieldService.this.reserve(target, damageId, damageType, amount, channels);
            }
            public void remove(UUID target, NexusId source, ShieldPoolSnapshot.RemovalReason reason) { DefaultShieldService.this.remove(target, source, reason); }
            public List<ShieldPoolSnapshot> snapshot(UUID target) { return DefaultShieldService.this.snapshot(target); }
            public List<ShieldPoolSnapshot> snapshot(UUID target, java.util.function.IntConsumer budget) { return DefaultShieldService.this.snapshot(target, budget); }
            public Reservation reserve(UUID target, long damageId, NexusId damageType, double amount) { return DefaultShieldService.this.reserve(target, damageId, damageType, amount); }
        };
    }
    private final CombatServerRuntime server;
    private final io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime world;
    private final CombatListeners<ShieldSettlement> listeners = new CombatListeners<>();
    private boolean closed;
    public DefaultShieldService(CombatServerRuntime server, io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime world, io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) { this.server = Objects.requireNonNull(server); this.world = Objects.requireNonNull(world); this.state = new ShieldPoolState(time); listeners.freeze(); }
    private final ShieldPoolState state;
    private final Map<ShieldPoolState.DamageKey, Token> tokens = new HashMap<>();
    private Set<NexusId> eligibleTypes(ShieldGrant grant) {
        var eligible = new HashSet<NexusId>();
        for (var id : grant.definition().damageTypes()) {
            if (!world.damageTypes().contains(id)) throw new IllegalArgumentException("Unknown shield damage type: " + id);
            eligible.add(id);
        }
        for (var id : grant.definition().damageTypeTags()) {
            eligible.addAll(world.damageTypes().tag(id).orElseThrow(() -> new IllegalArgumentException("Unknown shield damage tag: " + id)));
        }
        return Set.copyOf(eligible);
    }

    public List<SavedShield> save(UUID target, Set<NexusId> sources) {
        requireOpen(); requireNoPending(target);
        return state.save(target, sources).stream()
                .map(value -> new SavedShield(value.grant(), value.balance(), value.damageAge(), value.damaged())).toList();
    }
    public void restore(UUID target, List<SavedShield> saved, Set<NexusId> sources, Map<NexusId, NexusId> pools) {
        requireStarted();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        for (var value : saved) {
            var grant = value.grant();
            if (!sources.contains(grant.source().id()) && !pools.containsKey(grant.source().id())) continue;
            long now = world.businessTick(target, pools.getOrDefault(grant.source().id(),grant.source().id()));
            long remaining = grant.expiresAtTick().isPresent() ? grant.expiresAtTick().getAsLong() - now : -1;
            if (grant.expiresAtTick().isPresent() && remaining <= 0) continue;
            var source = new io.github.fatalitaya.combatnexus.combat.api.CombatSource(pools.getOrDefault(grant.source().id(), grant.source().id()), grant.source().owner(), Optional.empty());
            var restored = new ShieldGrant(source, grant.definition(), value.balance(), remaining < 0 ? OptionalLong.empty() : OptionalLong.of(Math.addExact(now, remaining)));
            state.restore(target, restored, eligibleTypes(restored), value.damaged(), now - value.damageAge());
        }
    }
    public void suspend(UUID target,NexusId source,boolean suspended){requireOpen();CombatMutationGuard.requireWorldWritePhase();state.suspend(target,source,suspended);}
    public void refresh(UUID target,NexusId source,ShieldDefinition definition){requireOpen();CombatMutationGuard.requireWorldWritePhase();
        var grant=new ShieldGrant(new io.github.fatalitaya.combatnexus.combat.api.CombatSource(source,Optional.empty(),Optional.empty()),definition,0,OptionalLong.empty());
        state.refresh(target,source,definition,eligibleTypes(grant));}
    @Override public List<ShieldPoolSnapshot> snapshot(UUID target) {
        requireOpen();
        return snapshot(target, count -> {});
    }
    @Override public List<ShieldPoolSnapshot> snapshot(UUID target, java.util.function.IntConsumer budget) {
        requireOpen();
        return state.snapshot(target, budget);
    }

    @Override public ShieldGrantReceipt replace(UUID target, ShieldGrant grant) { return replace(target, grant, null); }
    @Override public ShieldGrantReceipt replace(UUID target, ShieldGrant grant, CombatParent parent) {
        requireStarted();
        Objects.requireNonNull(grant, "grant");
        if (!world.targetAlive(Objects.requireNonNull(target, "target")))
            throw new IllegalArgumentException("Shield grant requires a live target");
        var captured = world.behaviors().capture(target);
        // Preserve preflight ordering: pending/expiry refusal precedes native type resolution.
        state.checkGrant(target, grant);
        var replacement = state.prepare(target, grant, eligibleTypes(grant));
        if (io.github.fatalitaya.combatnexus.combat.internal.transaction.StateApplicationScope.isApplying()) {
            var active = CombatExecutionScope.currentLineage(server.session()).orElseThrow();
            if (active.operation() != CombatOperation.SOURCE && active.operation() != CombatOperation.SHIELD)
                throw new IllegalStateException("State command cannot grant shields");
            server.ticket(active).bindTarget(world, target);
            return new ShieldGrantReceipt(active.id(), state.install(target, replacement));
        }
        CombatLineage lineage = server.requestLineage(CombatOperation.SHIELD, parent);
        if (!server.admit(lineage).admitted()) throw new IllegalStateException("Shield grant budget exhausted");
        server.ticket(lineage).bindTarget(world, target);
        try (var completion = server.reserveCompletion(lineage)) {
        double granted;
        try (var applying = server.ticket(lineage).enter(CombatExecutionScope.Phase.APPLICATION)) {
            granted = state.install(target, replacement);
        }
        publish(completion, lineage, new ShieldSettlement(target, ShieldSettlement.Kind.GRANTED, granted, OptionalLong.empty()), captured, new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal(List.of(),true));
        return new ShieldGrantReceipt(lineage.id(), granted);
        } finally { server.finish(lineage); }
    }

    @Override public void remove(UUID target, NexusId source, ShieldPoolSnapshot.RemovalReason reason) {
        requireOpen();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(CombatOperation.SHIELD);
        Objects.requireNonNull(reason, "reason");
        if (reason == ShieldPoolSnapshot.RemovalReason.BROKEN || reason == ShieldPoolSnapshot.RemovalReason.EXPIRED) {
            throw new IllegalArgumentException("Consumption and expiration are service-owned reasons");
        }
        requireNoPending(target);
        state.remove(target, source, reason);
    }

    public double previewRemaining(UUID target, long damageId, NexusId type, double amount, Map<NexusId, Float> channels) {
        requireStarted();
        if (!CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION))
            throw new IllegalStateException("Shield prediction requires application");
        var lineage = CombatExecutionScope.currentLineage(world.serverSession()).orElseThrow();
        server.ticket(lineage).bindTarget(world, target);
        Amounts.nonNegative(amount, "amount");
        var allocation = state.reserve(target, damageId, type, amount, ShieldPoolState.channels(amount, channels));
        try { return allocation.remaining; } finally { allocation.release(); }
    }
    @Override public Reservation reserve(UUID target, long damageId, NexusId damageType, double amount) {
        return reserve(target, damageId, damageType, amount, (CombatParent) null);
    }
    @Override public Reservation reserve(UUID target, long damageId, NexusId damageType, double amount, CombatParent parent) {
        return reserve(target, damageId, damageType, amount, Map.of(), parent);
    }
    @Override public Reservation reserve(UUID target, long damageId, NexusId damageType, double amount, Map<NexusId, Float> channels) {
        return reserve(target, damageId, damageType, amount, channels, null);
    }
    public Reservation reserveAfterReaction(UUID target, long damageId, NexusId type, double amount, Map<NexusId, Float> channels,
            double maximumAbsorption, Set<NexusId> existingSources) {
        if (!CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION))
            throw new IllegalStateException("Reactive shield allocation requires application");
        return reserve(target, damageId, type, amount, channels, null, maximumAbsorption, Set.copyOf(existingSources));
    }
    private Reservation reserve(UUID target, long damageId, NexusId damageType, double amount, Map<NexusId, Float> channels, CombatParent parent) {
        return reserve(target, damageId, damageType, amount, channels, parent, amount, Set.of());
    }
    private Reservation reserve(UUID target, long damageId, NexusId damageType, double amount, Map<NexusId, Float> channels, CombatParent parent,
            double maximumAbsorption, Set<NexusId> existingSources) {
        requireStarted();
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(damageType, "damageType");
        Amounts.nonNegative(amount, "amount");
        var channelRemainder = ShieldPoolState.channels(amount, channels);
        state.checkReservation(target, damageId, CombatExecutionScope.isActive());
        CombatLineage lineage = CombatExecutionScope.isActive()
                ? server.enlistedLineage(CombatOperation.SHIELD, damageId)
                : server.requestLineage(CombatOperation.SHIELD, parent);
        if (!CombatExecutionScope.isActive() && !server.admit(lineage).admitted()) throw new IllegalStateException("Shield consumption budget exhausted");
        server.ticket(lineage).bindTarget(world, target);
        var allocation = state.reserve(target, damageId, damageType, amount, channelRemainder, maximumAbsorption, existingSources);
        Token token = new Token(target, damageId, lineage, allocation);
        state.remember(allocation);
        tokens.put(new ShieldPoolState.DamageKey(target, damageId), token);
        return token;
    }

    private final class Token implements Reservation {
        private final CombatLineage lineage;
        private final long damageId;
        private final UUID target;
        private final ShieldPoolState.AllocationState allocation;
        private final List<io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource> captured;
        private final double absorbed;
        private final double remaining;
        private ShieldCommitReceipt receipt;
        @Override public Optional<ShieldCommitReceipt> receipt() { return Optional.ofNullable(receipt); }
        Token(UUID target, long damageId, CombatLineage lineage, ShieldPoolState.AllocationState allocation) {
            this.damageId = damageId;
            this.lineage = lineage;
            this.target = target;
            this.allocation = allocation;
            this.captured = world.behaviors().capture(target);
            this.absorbed = allocation.absorbed;
            this.remaining = allocation.remaining;
        }
        @Override public double absorbed() { return absorbed; }
        @Override public double remainingDamage() { return remaining; }
        @Override public ShieldCommitReceipt commit() {
            requireStarted();
            if (receipt != null) return receipt;
            if (!allocation.pending()) throw new IllegalStateException("Reservation released or expired");
            try (var completion = server.reserveCompletion(lineage)) {
            try (var applying = server.ticket(lineage).enter(CombatExecutionScope.Phase.APPLICATION)) {
                receipt = allocation.commit();
            }
            var writes = receipt.writes();
            if (absorbed > 0) publish(completion, lineage, new ShieldSettlement(target, ShieldSettlement.Kind.CONSUMED, absorbed, OptionalLong.of(damageId), writes.stream().filter(w->w.before().orElse(0)>0&&w.after().orElse(1)==0).flatMap(w->w.source().stream()).collect(java.util.stream.Collectors.toSet())), captured, new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal(writes,true));
            return receipt;
            } finally { if (lineage.operation() == CombatOperation.SHIELD) server.finish(lineage); }
        }
        @Override public void close() {
            server.requireThread();
            if (!allocation.pending()) return;
            allocation.release();
            if (lineage.operation() == CombatOperation.SHIELD) server.finish(lineage);
        }
    }

    private long reservationTick = Long.MIN_VALUE;
    @Override public void tick(long gameTick) {
        io.github.fatalitaya.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle();
        requireOpen();
        if (!state.advanceRequired(gameTick)) return;
        if (reservationTick != world.gameTick()) { tokens.values().forEach(Token::close); tokens.clear(); state.clearReservations(); reservationTick=world.gameTick(); }
        state.tick(gameTick);
    }

    private void requireOpen() {
        server.requireThread();
        if (closed || server.world(world.worldSession()).orElse(null) != world) throw new IllegalStateException("Shield world session closed");
    }
    private void publish(CombatCompletionQueue.Slot completion, CombatLineage lineage, ShieldSettlement settlement, List<io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource> captured, io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal journal) {
        CombatPublicationGate gate = new CombatPublicationGate();
        completion.publish(() -> {
            if (gate.claim()) {
                var identity=new io.github.fatalitaya.combatnexus.combat.api.CommandContext.Identity(lineage.id(),lineage.rootId(),lineage.parentId(),lineage.depth());
                io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.completed(world,lineage,new io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementEnvelope(CombatOperation.SHIELD,identity,settlement.target(),Optional.empty(),journal,settlement),captured);
                io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.shieldCompleted(server.session(), settlement);
                listeners.dispatch(server.session(), lineage, settlement, settlement.amount() > 0 ? EnumSet.allOf(CombatOperation.class) : Set.of());
            }
        });
        server.drain();
    }
    @Override public void close() { requireOpen(); reset(); listeners.clear(); closed = true; }
    private void requireStarted() {
        requireOpen();
        if (!state.started()) throw new IllegalStateException("Initialize world tick before using shields");
    }
    private void requireNoPending(UUID target) { state.requireNoPending(target); }
    @Override public void clear(UUID target) {
        io.github.fatalitaya.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle();
        requireOpen();
        // State retains damage identities until the next tick, including closed handles.
        tokens.values().forEach(token -> { if (token.target.equals(target)) token.close(); });
        state.clear(target);
    }
    @Override public void reset() {
        io.github.fatalitaya.combatnexus.combat.internal.transaction.WorldLifecycleScope.requireIdleOrLifecycle();
        requireOpen();
        tokens.values().forEach(Token::close); tokens.clear();
        state.reset();
    }
    @Override public List<Change> drainChanges() {
        requireOpen();
        return state.drainChanges().stream().map(change -> new Change(change.target(), change.pool(), change.reason())).toList();
    }
}
