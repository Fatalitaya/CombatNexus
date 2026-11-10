package io.github.fatalitaya.combatnexus.effect.internal;
import io.github.fatalitaya.combatnexus.effect.spi.EffectContributionPort;

import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
import java.util.function.Predicate;

/** Source identity is (target, effect, source); removing one source never removes another's control. */
public final class DefaultEffectService implements EffectServiceControl {
    public EffectService businessView() {
        return new EffectService() {
            public boolean bind(EffectBinding binding) { return DefaultEffectService.this.bind(binding); }
            public boolean apply(EffectInstance effect, NexusId stacking) { return DefaultEffectService.this.apply(effect, stacking); }
            public io.github.fatalitaya.combatnexus.combat.api.CommandResult<Boolean> applyInstant(EffectInstance effect) { return DefaultEffectService.this.applyInstant(effect); }
            public void remove(UUID target, NexusId effect, NexusId source, Removal reason) { DefaultEffectService.this.remove(target, effect, source, reason); }
            public List<EffectInstance> snapshot(UUID target) { return DefaultEffectService.this.snapshot(target); }
            public List<EffectInstance> snapshot(UUID target, java.util.function.IntConsumer budget) { return DefaultEffectService.this.snapshot(target, budget); }
            public boolean immune(UUID target,NexusId effect,Set<NexusId> tags,Set<ControlCategory> categories){return DefaultEffectService.this.immune(target,effect,tags,categories);}
            public List<EffectCleanse.Entry> cleanse(EffectCleanse request){return DefaultEffectService.this.cleanse(request);}
            public int restrictionMask(UUID target) { return DefaultEffectService.this.restrictionMask(target); }
            public Set<NexusId> pendingSources(UUID target) { return DefaultEffectService.this.pendingSources(target); }
        };
    }
    public boolean immune(UUID target,NexusId effect,Set<NexusId> tags,Set<ControlCategory> categories){
        requireOpen();
        return group(target).values().stream().filter(e->!suspended(target,e.snapshot().source())&&!pending.containsKey(key(e)))
            .anyMatch(e->e.policy().control().matches(effect,tags,categories));
    }
    public List<EffectCleanse.Entry> cleanse(EffectCleanse request){
        requireOpen();requireGameplayWrite();var results=new ArrayList<EffectCleanse.Entry>();
        for(var previous:List.copyOf(group(request.target()).values())){
            var layers=new ArrayList<EffectInstance>();layers.add(previous);layers.addAll(previous.hidden());
            var retained=new ArrayList<EffectInstance>();boolean changed=false;
            for(int i=0;i<layers.size();i++){
                var layer=layers.get(i);boolean matches=request.matches(layer),remove=matches&&layer.policy().dispellable();
                if(matches)results.add(new EffectCleanse.Entry(layer.snapshot().effect(),layer.snapshot().source(),i>0,remove));
                if(remove)changed=true;
                else retained.add(new EffectInstance(layer.snapshot(),layer.attributes(),layer.ownership(),layer.behavior(),layer.policy(),List.of(),layer.display(),layer.stacking()));
            }
            if(!changed)continue;
            var key=key(previous);List<EffectInstance> after=List.of();
            if(!retained.isEmpty()){
                var first=retained.get(0);
                after=List.of(new EffectInstance(first.snapshot(),first.attributes(),first.ownership(),first.behavior(),first.policy(),retained.subList(1,retained.size()),first.display(),first.stacking()));
            }
            var change=new Change(List.of(previous),after,Optional.of(Removal.DISPELLED));commit(change);
            expiry.cancel(key);
            if(after.isEmpty()){active.get(request.target()).remove(key);insertionOrder.remove(key);order.remove(key);}
            else{var value=after.get(0);active.get(request.target()).put(key,value);value.snapshot().expiresAtTick().ifPresent(deadline->expiry.schedule(key,deadline));}
            updateRestrictions(request.target());changes.add(change);executor.committed(change);
        }
        return List.copyOf(results);
    }
    private record SuspendedSource(UUID target,NexusId source) {}
    private final Set<SuspendedSource> suspended = new HashSet<>();
    public boolean suspended(UUID target,NexusId source){return suspended.contains(new SuspendedSource(target,source));}
    public void suspend(UUID target,NexusId source,boolean pause){
        requireOpen();requireGameplayWrite();
        var identity=new SuspendedSource(target,source);
        if(suspended.contains(identity)==pause)return;
        for(var effect:group(target).values())if(effect.snapshot().source().equals(source)){
            var k=key(effect);reconcile(Set.of(k),true);
            var prepared=executor.suspend(effect,pause);var receipt=prepared.apply();
            if(!receipt.completed()){pending.put(k,prepared);throw new IllegalStateException("Effect suspension pending",receipt.failure());}
        }
        if(pause)suspended.add(identity);else suspended.remove(identity);
        updateRestrictions(target);
    }
    private record Key(UUID target, NexusId effect, NexusId source) {}
    private final Map<UUID, Map<Key, EffectInstance>> active = new LinkedHashMap<>();
    private final Set<Key> insertionOrder = new LinkedHashSet<>();
    private Map<Key, EffectInstance> group(UUID target) { return active.getOrDefault(target, Map.of()); }
    private List<EffectInstance> allActive() { return insertionOrder.stream().map(k -> group(k.target()).get(k)).toList(); }
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.DeadlineIndex<Key> expiry = new io.github.fatalitaya.combatnexus.combat.internal.coordination.DeadlineIndex<>();
    private final Map<Key, Long> order = new HashMap<>();
    private long sequence;
    private final Map<UUID, Integer> restrictionMasks = new HashMap<>();
    private void updateRestrictions(UUID target) {
        int mask = 0;
        for (var effect : group(target).values())
            if(!suspended(target,effect.snapshot().source())) for (var action : effect.snapshot().restrictions()) mask |= 1 << action.ordinal();
        if (mask == 0) restrictionMasks.remove(target); else restrictionMasks.put(target, mask);
    }
    private final List<Change> changes = new ArrayList<>();
    private final Predicate<EffectInstance> immunity;
    private long tick = -1;
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    private boolean lifecycle;
    private boolean bindingUpdate;
    private boolean restoring;
    public void restoring(boolean value){requireOpen();requireGameplayWrite();restoring=value;}
    @Override public boolean bind(EffectBinding binding) {
        requireOpen(); requireGameplayWrite();
        var s = binding.effect().snapshot();
        if (!binding.satisfied()) { remove(s.target(), s.effect(), s.source(), Removal.CONDITION_ENDED); return true; }
        boolean previous = bindingUpdate; bindingUpdate = true;
        try {
            boolean applied = apply(binding.effect(), binding.stacking());
            if (!applied) remove(s.target(), s.effect(), s.source(), Removal.CONDITION_ENDED);
            return applied;
        } finally { bindingUpdate = previous; }
    }
    private void requireGameplayWrite() {
        if (!lifecycle) io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.EFFECT);
        if (!lifecycle && io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.isExecuting()
                && !io.github.fatalitaya.combatnexus.combat.internal.transaction.StateApplicationScope.isApplying())
            throw new IllegalStateException("Effect gameplay writes require a non-computing world phase");
    }
    private final EffectContributionPort executor;
    private void requireOpen() {
        if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Effect session closed or wrong thread");
    }
    public Set<io.github.fatalitaya.combatnexus.api.id.NexusId> persistentSources(UUID target) {
        return snapshot(target).stream().filter(e -> e.display().persistent()
                && (!e.display().environment() || e.snapshot().lifetime() == EffectLifetime.Kind.TIMED))
                .map(e -> e.snapshot().source()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public List<EffectInstance> save(UUID target, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> sources) {
        return snapshot(target).stream().filter(e -> e.ownership() == io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED
                && sources.contains(e.snapshot().source())
                && !(e.display().environment() && e.snapshot().lifetime() == EffectLifetime.Kind.PERMANENT)).toList();
    }
    public void restore(UUID target, List<EffectInstance> saved, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> sources) {
        requireOpen(); requireGameplayWrite();
        for (var value : saved) {
            if (!value.snapshot().target().equals(target)) throw new IllegalArgumentException("Foreign persistent effect");
            if (!sources.contains(value.snapshot().source())) continue;
            EffectLayers.active(value, time)
                    .ifPresent(effect -> apply(effect, effect.stacking()));
        }
    }
    @Override public void close() { requireOpen(); reset(); closed = true; }

    public DefaultEffectService(Predicate<EffectInstance> immunity, io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) {
        this(immunity, change -> {
            if (java.util.stream.Stream.concat(change.before().stream(), change.after().stream())
                    .anyMatch(value -> value.ownership() == io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED))
                throw new IllegalStateException("Managed effects require a contribution port");
            return new EffectContributionPort.Prepared() {
                public EffectContributionPort.Receipt apply() { return new EffectContributionPort.Receipt(List.of(), true, null); }
                public EffectContributionPort.Receipt reconcile() { return apply(); }
            };
        }, time);
    }
    private final io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time;
    public DefaultEffectService(Predicate<EffectInstance> immunity, EffectContributionPort executor, io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) {
        this.time = Objects.requireNonNull(time);
        this.immunity = Objects.requireNonNull(immunity, "immunity");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    private final Map<Key, EffectContributionPort.Prepared> pending = new LinkedHashMap<>();
    private static Key key(EffectInstance effect) {
        var value = effect.snapshot(); return new Key(value.target(), value.effect(), effect.display().aggregateSources()
                ? NexusId.fromNamespaceAndPath("combatnexus", "aggregate") : value.source());
    }
    private void reconcile(Set<Key> keys, boolean required) {
        for (var key : keys) {
            var prepared = pending.get(key);
            if (prepared == null) continue;
            try {
                var receipt = prepared.reconcile();
                if (receipt.completed()) { pending.remove(key); updateRestrictions(key.target()); }

            } catch (RuntimeException failure) {
                if (required) throw new IllegalStateException("Effect source reconciliation failed", failure);
            }
        }
        if (required && keys.stream().anyMatch(pending::containsKey))
            throw new IllegalStateException("Effect source awaits reconciliation");
    }
    private void commit(Change change) {
        Set<Key> keys = new LinkedHashSet<>();
        change.before().forEach(value -> keys.add(key(value)));
        change.after().forEach(value -> keys.add(key(value)));
        reconcile(keys, true);
        var prepared = executor.prepare(change);
        var receipt = prepared.apply();
        if (!receipt.completed()) {
            keys.forEach(key -> pending.put(key, prepared));
            reconcile(keys, false);
            throw new IllegalStateException("Effect contribution application incomplete", receipt.failure());
        }
    }
    @Override public Set<NexusId> pendingSources(UUID target) {
        requireOpen();
        return pending.keySet().stream().filter(key -> key.target().equals(target))
                .map(Key::source).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override public boolean apply(EffectInstance effect, NexusId stacking) {
        requireOpen();
        requireGameplayWrite();
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(stacking, "stacking");
        var adoptedStacking = lifecycle || bindingUpdate && stacking.equals(Stacking.REPLACE.id()) ? effect.stacking() : stacking;
        effect = effect.withStacking(adoptedStacking);
        if (tick < 0) throw new IllegalStateException("Initialize world tick before applying effects");
        var snapshot = effect.snapshot();
        if (expired(snapshot)) return false;
        try (var compute = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.isActive()
                ? io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.computation() : null) {
            if (!restoring && effect.policy().application()!=EffectApplicationPolicy.FORCE && (immunity.test(effect)||immune(effect.snapshot().target(),effect.snapshot().effect(),effect.policy().tags(),effect.policy().control().categories()))) return false;
        }
        if (snapshot.lifetime() == EffectLifetime.Kind.INSTANT) return executor.instant(effect).applied();
        Key key = key(effect);
        reconcile(Set.of(key), true);
        EffectInstance previous = group(key.target()).get(key);
        if (previous != null && previous.ownership() != effect.ownership()) {
            throw new IllegalStateException("Revoke old execution owner before replacing it");
        }
        var strategy = EffectStackingStrategies.get(stacking);
        if (previous != null) {
            long now=time.now(effect.snapshot().target(),effect.snapshot().source());
            io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime reference=(target,source)->now;
            effect = EffectLayers.reclock(Objects.requireNonNull(strategy.resolve(
                EffectLayers.reclock(previous,time,reference),EffectLayers.reclock(effect,time,reference),now)),reference,time);
            if (!key(effect).equals(key)) throw new IllegalArgumentException("Stacking changed installation identity");
        }
        effect = effect.withStacking(adoptedStacking);
        if (effect.equals(previous)) return true;
        Change change = new Change(previous == null ? List.of() : List.of(previous), List.of(effect), Optional.empty(), bindingUpdate);
        commit(change);
        active.computeIfAbsent(key.target(), ignored -> new LinkedHashMap<>()).put(key, effect);
        insertionOrder.add(key);
        order.computeIfAbsent(key, ignored -> sequence++);
        expiry.cancel(key);
        effect.snapshot().expiresAtTick().ifPresent(deadline -> expiry.schedule(key, deadline));
        updateRestrictions(key.target());
        changes.add(change);
        executor.committed(change);
        executor.activated(effect);
        return true;
    }

    @Override public io.github.fatalitaya.combatnexus.combat.api.CommandResult<Boolean> applyInstant(EffectInstance effect) {
        requireOpen(); requireGameplayWrite();
        if (tick < 0) throw new IllegalStateException("Initialize world tick before applying effects");
        if (effect.snapshot().lifetime() != EffectLifetime.Kind.INSTANT)
            throw new IllegalArgumentException("Expected an instant effect");
        try (var compute = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.isActive()
                ? io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.computation() : null) {
            if (!restoring && effect.policy().application()!=EffectApplicationPolicy.FORCE && (immunity.test(effect)||immune(effect.snapshot().target(),effect.snapshot().effect(),effect.policy().tags(),effect.policy().control().categories()))) return io.github.fatalitaya.combatnexus.combat.api.CommandResult.rejected(
                    io.github.fatalitaya.combatnexus.combat.api.CommandRejection.Code.IMMUNE, "Effect immunity rejected application");
        }
        return executor.instant(effect);
    }

    @Override public void remove(UUID target, NexusId effect, NexusId source, Removal reason) {
        requireOpen();
        requireGameplayWrite();
        Objects.requireNonNull(reason, "reason");
        Key key = new Key(target, effect, source);
        var aggregate = new Key(target, effect, NexusId.fromNamespaceAndPath("combatnexus", "aggregate"));
        var grouped = group(target).get(aggregate);
        if (grouped != null && (grouped.snapshot().source().equals(source)
                || grouped.hidden().stream().anyMatch(layer -> layer.snapshot().source().equals(source)))) key = aggregate;
        reconcile(Set.of(key), true);
        EffectInstance previous = group(key.target()).get(key);
        if (previous != null) {
            if (previous.display().aggregateSources() && reason == Removal.REMOVED) {
                io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime zero=(t,s)->0;
                var remaining = EffectLayers.withoutSource(EffectLayers.reclock(previous,time,zero), source, 0)
                    .map(value->EffectLayers.reclock(value,zero,time));
                if (remaining.isPresent()) {
                    boolean oldBinding = bindingUpdate; bindingUpdate = true;
                    try { apply(remaining.get(), Stacking.REPLACE); }
                    finally { bindingUpdate = oldBinding; }
                    return;
                }
            }
            if (reason == Removal.DISPELLED && !previous.policy().dispellable())
                throw new IllegalArgumentException("Effect is not dispellable");
            Change change = new Change(List.of(previous), List.of(), Optional.of(reason));
            commit(change);
            var group = active.get(target);
            group.remove(key);
            insertionOrder.remove(key);
            expiry.cancel(key); order.remove(key);
            updateRestrictions(target);
            if (group.isEmpty()) active.remove(target);
            changes.add(change);
            executor.committed(change);
        }
    }

    @Override public void tick(long gameTick) {
        requireOpen();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        if (gameTick < 0 || gameTick < tick) throw new IllegalArgumentException("Invalid world clock");
        if (gameTick == tick) return;
        reconcile(new LinkedHashSet<>(pending.keySet()), false);
        tick = gameTick;
        boolean beforeLifecycle = lifecycle;
        lifecycle = true;
        try {
        var due = expiry.due(k -> time.advancing(k.target(), group(k.target()).get(k).snapshot().source()) ? time.now(k.target(), group(k.target()).get(k).snapshot().source()) : Long.MIN_VALUE);
        due.sort(Comparator.comparingLong(order::get));
        for (var dueKey : due) {
            var effect = group(dueKey.target()).get(dueKey);
            if (effect == null) continue;
            var snapshot = effect.snapshot();
            if (!time.mayExecute()) continue;
            if (expired(snapshot) && !pending.containsKey(key(effect))) {
                var restored = EffectLayers.active(effect, time);
                if (restored.isPresent()) apply(restored.orElseThrow(), Stacking.REPLACE);
                else if (executor.mayExpire(effect)) remove(snapshot.target(), snapshot.effect(), snapshot.source(), Removal.EXPIRED);
            }
        }
        } finally { lifecycle = beforeLifecycle; }
    }

    private boolean expired(EffectSnapshot snapshot) {
        return snapshot.expiresAtTick().isPresent() && snapshot.expiresAtTick().getAsLong() <= time.now(snapshot.target(), snapshot.source());
    }

    @Override public List<EffectInstance> snapshot(UUID target) {
        requireOpen();
        return snapshot(target, count -> {});
    }
    @Override public List<EffectInstance> snapshot(UUID target, java.util.function.IntConsumer budget) {
        requireOpen(); List<EffectInstance> result = new ArrayList<>();
        for (var effect : group(target).values()) {
            budget.accept(1); result.add(effect);
        }
        return List.copyOf(result);
    }

    @Override public int restrictionMask(UUID target) {
        requireOpen(); return restrictionMasks.getOrDefault(target, 0);
    }

    @Override public void clear(UUID target, Removal reason) {
        requireOpen();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        Objects.requireNonNull(reason, "reason");
        boolean beforeLifecycle = lifecycle; lifecycle = true;
        try {
            reconcile(pending.keySet().stream().filter(key -> key.target().equals(target)).collect(java.util.stream.Collectors.toSet()), true);
            for (EffectInstance effect : snapshot(target)) {
                if (reason == Removal.DISPELLED && !effect.policy().dispellable()) continue;
                var snapshot = effect.snapshot();
                remove(target, snapshot.effect(), snapshot.source(), reason);
            }
        } finally { lifecycle = beforeLifecycle; }
        suspended.removeIf(s->s.target().equals(target));
    }

    /** World shutdown only; consumers must revoke their managed contributions before dropping the session. */
    @Override public void reset() {
        requireOpen();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        reconcile(Set.copyOf(pending.keySet()), true);
        boolean beforeLifecycle = lifecycle; lifecycle = true;
        try {
        for (var effect : allActive()) {
            var snapshot = effect.snapshot();
            remove(snapshot.target(), snapshot.effect(), snapshot.source(), Removal.WORLD_EXIT);
        }
        changes.clear(); suspended.clear(); tick = -1;
        } finally { lifecycle = beforeLifecycle; }
    }
    @Override public List<Change> drainChanges() {
        requireOpen();
        List<Change> result = List.copyOf(changes);
        changes.clear();
        return result;
    }
}
