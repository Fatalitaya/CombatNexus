package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import io.github.fatalitaya.combatnexus.combat.internal.persistence.SavedBehavior;
import io.github.fatalitaya.combatnexus.effect.spi.EffectContributionPort;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard;
import io.github.fatalitaya.combatnexus.effect.api.*;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Composes behavior writes with the existing native receipt and compensation protocol. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class WorldBehaviorSources implements BehaviorSources {
    private record Key(UUID target, NexusId effect, NexusId source) {}
    private final CombatRuntime world;
    private final io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time;
    private final Map<UUID, Map<Key, BehaviorSource>> active = new LinkedHashMap<>();
    private Map<Key, BehaviorSource> group(UUID target) { return active.getOrDefault(target, Map.of()); }
    private final ProtectionExecutor protection = new ProtectionExecutor();
    private final PeriodicBehaviorExecutor periodic;
    private final CombatReactions reactions;
    private final Map<UUID, List<BehaviorSource>> ordered = new HashMap<>();
    private final Set<Key> blocked = new HashSet<>();
    private record SuspendedSource(UUID target,NexusId source) {}
    private final Set<SuspendedSource> suspended = new HashSet<>();
    private boolean restoring;
    public void restoring(boolean value){check();CombatMutationGuard.requireWorldWritePhase();restoring=value;}
    public void suspend(UUID target,NexusId source,boolean pause){
        check();CombatMutationGuard.requireWorldWritePhase();var key=new SuspendedSource(target,source);
        if(pause)suspended.add(key);else suspended.remove(key);ordered.remove(target);
        for(var s:group(target).values())if(s.source().equals(source))for(var pool:s.plan().ownedShields(s).values())world.shieldControl().suspend(target,pool,pause);
    }
    private boolean suspended(UUID target,NexusId source){return suspended.contains(new SuspendedSource(target,source));}
    private EffectInstance attributes(EffectInstance effect,boolean pause){return pause?new EffectInstance(effect.snapshot(),List.of(),effect.ownership(),effect.behavior(),effect.policy(),effect.hidden(),effect.display(),effect.stacking()):effect;}
    private EffectInstance attributes(EffectInstance effect){return attributes(effect,suspended(effect.snapshot().target(),effect.snapshot().source()));}
    private long sequence;
    private long lastTick = -1;
    private boolean closed;
    public WorldBehaviorSources(CombatRuntime world, io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) { this(world, new CombatReactions(world), time); }
    public WorldBehaviorSources(CombatRuntime world, CombatReactions reactions, io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) {
        this.time = Objects.requireNonNull(time);
        this.world = world; this.reactions = reactions; periodic = new PeriodicBehaviorExecutor(world, time);
    }
    /** Return a separate read facade, never the installation executor. */
    public BehaviorSources readOnly() {
        return new BehaviorSources() {
            public List<BehaviorSource> capture(UUID target) { return WorldBehaviorSources.this.capture(target); }
            public List<BehaviorView> view(UUID target) { return WorldBehaviorSources.this.view(target); }
            public boolean valid(BehaviorSource source) { return WorldBehaviorSources.this.valid(source); }
        };
    }
    private void check() { if (closed) throw new IllegalStateException("Behavior session closed"); world.requireActive(); }
    private static Key key(EffectInstance e) { var s = e.snapshot(); return new Key(s.target(), s.effect(), s.source()); }
    private static Key key(BehaviorSource s) { return new Key(s.target(), s.effect(), s.source()); }
    @Override public List<BehaviorSource> capture(UUID target) {
        check(); return ordered.computeIfAbsent(target, ignored -> group(target).values().stream().filter(s -> !blocked.contains(key(s)) && !suspended(s.target(),s.source())).sorted(
                Comparator.comparingInt((BehaviorSource s) -> s.plan().priority()).reversed()
                .thenComparing(s -> s.source().toString()).thenComparingLong(BehaviorSource::installation)).toList());
    }
    @Override public List<BehaviorView> view(UUID target) { return view(target, count -> {}); }
    public List<BehaviorView> view(UUID target, java.util.function.IntConsumer budget) {
        check(); var result = new ArrayList<BehaviorView>();
        for (var source : capture(target)) {
            budget.accept(1); result.add(new BehaviorView(source, protectionAvailable(source)));
        }
        return List.copyOf(result);
    }
    @Override public boolean valid(BehaviorSource source) {
        check(); return !suspended(source.target(),source.source()) && present(source);
    }
    private boolean present(BehaviorSource source){
        return !blocked.contains(key(source)) && source.session().equals(world.sessionId()) && source.equals(group(source.target()).get(key(source)))
                && (source.expiresAtTick().isEmpty() || source.expiresAtTick().getAsLong() > time.now(source.target(), source.source()));
    }
    public io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision protect(
            UUID target, List<BehaviorSource> captured, float before, float proposed, io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs inputs, java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.CombatSource> origin) {
        check(); return protection.decide(world, this, target, captured, before, proposed, inputs, origin);
    }
    public GuardDecision guard(io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardContext context,
            List<BehaviorSource> captured) {
        check(); return protection.guard(world, this, context, captured);
    }
    public record GuardDecision(boolean selected, BeforeApplicationPlan plan, Optional<ApplicationJournal.Entry> consumption) {
        public static final GuardDecision NONE = new GuardDecision(false, BeforeApplicationPlan.EMPTY, Optional.empty());
    }
    private boolean protectionAvailable(BehaviorSource source) { return valid(source) && protection.available(source, time.now(source.target(), source.source())); }
    public EffectContributionPort compose(EffectContributionPort attributes) {
        return new EffectContributionPort() {
          public Prepared suspend(EffectInstance effect,boolean pause){
            return attributes.prepare(new EffectService.Change(List.of(attributes(effect,!pause)),List.of(attributes(effect,pause)),Optional.empty(),true));
          }
          public void committed(EffectService.Change change) {
            attributes.committed(change);
            var before = change.before().stream().findFirst();
            var after = change.after().stream().findFirst();
            var identity = after.or(() -> before).orElseThrow();
            var oldState = before.map(WorldBehaviorSources.this::momentState);
            var newState = after.map(WorldBehaviorSources.this::momentState);
            var kind = before.isEmpty() ? io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.EffectChange.ADDED
                : after.isEmpty() ? io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.EffectChange.REMOVED
                : after.get().snapshot().level() > before.get().snapshot().level() ? io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.EffectChange.UPGRADED
                : after.get().snapshot().level() == before.get().snapshot().level() ? io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.EffectChange.REFRESHED
                : io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.EffectChange.CHANGED;
            io.github.fatalitaya.combatnexus.combat.spi.CombatMomentAccess.publish(world.level(), new io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.Effect(
                identity.snapshot().target(), identity.snapshot().effect(), oldState, newState, kind, Optional.ofNullable(identity.display().owner()), Optional.of(change)));
            if (!restoring && !change.preservePeriodicPhase() && !suspended(identity.snapshot().target(),identity.snapshot().source()) && world.level().getEntity(identity.snapshot().target()) instanceof net.minecraft.world.entity.LivingEntity entity) {
                if (after.isEmpty()) before.ifPresent(effect -> EffectTriggers.dispatch(entity, effect, EffectCapability.Trigger.END, null, 0));
                else after.ifPresent(effect -> EffectTriggers.dispatch(entity, effect, EffectCapability.Trigger.APPLY, null, 0));
            }
          }
          public boolean mayExpire(EffectInstance effect) { return attributes.mayExpire(effect); }
          public void activated(EffectInstance effect) {
            var source = group(effect.snapshot().target()).get(key(effect));
            if (source != null && !restoring)
                periodic.tick(world.gameTick(), WorldBehaviorSources.this::present,WorldBehaviorSources.this::valid);
          }
          public CommandResult<Boolean> instant(EffectInstance effect) {
            check(); CombatMutationGuard.requireWorldWritePhase();
            var s = effect.snapshot();
            return periodic.instant(new BehaviorSource(world.sessionId(), s.target(), s.effect(), s.source(),
                ++sequence, OptionalLong.empty(), effect.behavior(), Optional.ofNullable(effect.display().owner())
                    .or(() -> world.sourceInstallations().identity(s.target(), s.source()).flatMap(origin -> origin.owner()))));
          }
          public Prepared prepare(EffectService.Change change) {
            check(); CombatMutationGuard.requireWorldWritePhase();
            var preparedAt = new HashMap<Key, Long>();
            java.util.stream.Stream.concat(change.before().stream(), change.after().stream()).forEach(e -> preparedAt.put(key(e), time.now(e.snapshot().target(), e.snapshot().source())));
            var nativePrepared = attributes.prepare(new EffectService.Change(change.before().stream().map(WorldBehaviorSources.this::attributes).toList(),change.after().stream().map(WorldBehaviorSources.this::attributes).toList(),change.removal(),change.preservePeriodicPhase()));
            Map<Key, BehaviorSource> before = new LinkedHashMap<>(), after = new LinkedHashMap<>();
            for (var e : change.before()) { var k = key(e); before.put(k, group(k.target()).get(k)); after.put(k, null); }
            for (var e : change.after()) {
                var k = key(e); before.putIfAbsent(k, group(k.target()).get(k));
                var s = e.snapshot();
                var old = before.get(k);
                var owner = Optional.ofNullable(e.display().owner()).or(() -> world.sourceInstallations().identity(s.target(), s.source()).flatMap(origin -> origin.owner()));
                after.put(k, e.behavior().empty() ? null : old != null && old.plan().equals(e.behavior())
                        && old.expiresAtTick().equals(s.expiresAtTick()) && old.owner().equals(owner) ? old
                        : new BehaviorSource(world.sessionId(), s.target(), s.effect(), s.source(), change.preservePeriodicPhase() && old != null ? old.installation() : ++sequence, s.expiresAtTick(), e.behavior(), owner));
            }
            var beforeConsumed = new HashMap<Long, Map<NexusId, Integer>>();
            var beforeCooldowns = new HashMap<Long, Map<NexusId, Long>>();
            for (var source : before.values()) if (source != null) beforeCooldowns.put(source.installation(), protection.remaining(source.installation(), preparedAt.get(key(source))));
            var beforePeriodic = new HashMap<Long, Map<NexusId, Long>>();
            for (var source : before.values()) if (source != null)
                beforePeriodic.put(source.installation(), periodic.remaining(source.installation(), preparedAt.get(key(source))));
            for (var source : before.values()) if (source != null) beforeConsumed.put(source.installation(), protection.consumed(source.installation()));
            var beforeTriggers = before.values().stream().filter(Objects::nonNull).map(s -> reactions.checkpoint(s.installation())).toList();
            var proposed = new HashMap<Key, BehaviorSource>();
            active.values().forEach(proposed::putAll);
            after.forEach((k,v) -> { if (v == null) proposed.remove(k); else proposed.put(k,v); });
            var reserved = new HashSet<>(proposed.keySet());
            reserved.addAll(blocked); // Failed installations still occupy bounded reconciliation slots.
            var counts = new HashMap<UUID, Integer>();
            for (var key : reserved) if (counts.merge(key.target(), 1, Integer::sum) > 64) throw new IllegalStateException("Behavior source capacity exceeded");
            periodic.validate(proposed.values(), world.gameTick());
            return new EffectContributionPort.Prepared() {
                public EffectContributionPort.Receipt apply() {
                    check(); CombatMutationGuard.requireWorldWritePhase();
                    block(after.keySet());
                    try {
                        var receipt = nativePrepared.apply();
                        if (!receipt.completed()) return receipt;
                        var applied = write(after, receipt);
                        if (applied.completed()) for (var entry : after.entrySet()) {
                            var source = entry.getValue();
                            if (source != null && source.equals(before.get(entry.getKey())))
                                periodic.restore(source, beforePeriodic.get(source.installation()), preparedAt.get(key(source)));
                            else if (source != null) {
                                var previous = before.get(entry.getKey());
                                // An aggregate replacement explicitly identifies its predecessor even when
                                // ownership changes the source key. Transfer remaining delay, not clock time.
                                if (previous == null && change.before().size() == 1 && change.after().size() == 1
                                        && change.before().get(0).display().aggregateSources()
                                        && change.after().get(0).display().aggregateSources())
                                    previous = before.get(key(change.before().get(0)));
                                if (previous != null)
                                    periodic.refresh(source, beforePeriodic.get(previous.installation()), preparedAt.get(key(source)), change.preservePeriodicPhase());
                            }
                        }
                        if (applied.completed()) { blocked.removeAll(after.keySet()); after.keySet().forEach(k -> ordered.remove(k.target())); }
                        return applied;
                    } catch (RuntimeException failure) { return new EffectContributionPort.Receipt(List.of(), false, failure); }
                }
                public EffectContributionPort.Receipt reconcile() {
                    check(); CombatMutationGuard.requireWorldWritePhase();
                    block(after.keySet());
                    try {
                        var receipt = nativePrepared.reconcile();
                        if (!receipt.completed()) return receipt;
                        for (var checkpoint : beforeTriggers) reactions.restore(checkpoint);
                        var restored = write(before, receipt);
                        if (restored.completed()) for (var source : before.values()) if (source != null)
                            periodic.restore(source, beforePeriodic.get(source.installation()), preparedAt.get(key(source)));
                        for (var source : before.values()) if (source != null && beforeConsumed.containsKey(source.installation())) protection.restore(source.installation(), beforeConsumed.get(source.installation()), beforeCooldowns.get(source.installation()), preparedAt.get(key(source)));
                        if (restored.completed()) { blocked.removeAll(after.keySet()); after.keySet().forEach(k -> ordered.remove(k.target())); }
                        return restored;
                    } catch (RuntimeException failure) { return new EffectContributionPort.Receipt(List.of(), false, failure); }
                }
                private EffectContributionPort.Receipt write(Map<Key, BehaviorSource> desired, EffectContributionPort.Receipt receipt) {
                    List<EffectContributionPort.Write> writes = new ArrayList<>(receipt.writes());
                    try {
                        desired.forEach((k,v) -> { ordered.remove(k.target()); if (Objects.equals(group(k.target()).get(k), v)) { if (v != null) periodic.install(v, time.now(v.target(), v.source())); return; }
                            var previous=group(k.target()).get(k);
                            if(change.preservePeriodicPhase() && previous!=null && v!=null && previous.installation()==v.installation()){
                                var retained=v.plan().components().stream().map(BehaviorComponent::type).collect(java.util.stream.Collectors.toSet());
                                for(var component:previous.plan().components())if(!retained.contains(component.type()))component.revoke(previous,pool->world.shields().remove(previous.target(),pool,io.github.fatalitaya.combatnexus.combat.shield.api.ShieldPoolSnapshot.RemovalReason.REMOVED));
                                protection.retain(previous.installation(), retained);
                                var reactionKeys=retained.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet());
                                for(var component:v.plan().components()){
                                    if(component instanceof io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.Periodic ability)reactionKeys.add(ability.contribution().toString());
                                    if(component instanceof io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.Triggered ability)reactionKeys.add(ability.contribution().toString());
                                }
                                reactions.retain(previous.installation(),reactionKeys);
                                periodic.cancel(previous.installation());
                            }else remove(k);
                            if (v != null) { active.computeIfAbsent(k.target(), ignored -> new LinkedHashMap<>()).put(k,v); periodic.install(v, time.now(v.target(), v.source())); }
                            writes.add(new EffectContributionPort.Write("behavior/" + k.source(), v != null)); });
                        return new EffectContributionPort.Receipt(writes, true, null);
                    } catch (RuntimeException failure) { return new EffectContributionPort.Receipt(writes, false, failure); }
                }
            };
          }
        };
    }
    private io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.EffectState momentState(EffectInstance effect) {
        var state = effect.snapshot();
        return new io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.EffectState(state.expiresAtTick().isEmpty() ? -1
            : (int)Math.min(Integer.MAX_VALUE, Math.max(0, state.expiresAtTick().getAsLong() - time.now(state.target(), state.source()))), state.level());
    }
    private void block(Set<Key> keys) {
        blocked.addAll(keys); keys.forEach(k -> ordered.remove(k.target()));
        for (var key : keys) {
            var source = group(key.target()).get(key);
            if (source == null) continue;
            periodic.cancel(source.installation());
        }
    }
    private void remove(Key key) {
        ordered.remove(key.target());
        var old = group(key.target()).get(key); if (old == null) return;
        for (var component : old.plan().components()) component.revoke(old, pool -> world.shields().remove(old.target(), pool, io.github.fatalitaya.combatnexus.combat.shield.api.ShieldPoolSnapshot.RemovalReason.REMOVED));
        var group = active.get(key.target()); group.remove(key); if (group.isEmpty()) active.remove(key.target());
        periodic.cancel(old.installation());
        protection.remove(old.installation());
        reactions.removeSource(old.installation());
    }
    public void tick(long now) {
        check(); CombatMutationGuard.requireWorldWritePhase();
        if (now < lastTick) throw new IllegalArgumentException("Behavior clock moved backwards");
        if (now == lastTick) return; lastTick = now;
        for (var s : active.values().stream().flatMap(g -> g.values().stream()).toList()) if (!blocked.contains(key(s)) && s.expiresAtTick().isPresent() && s.expiresAtTick().getAsLong() <= time.now(s.target(), s.source()) && time.advancing(s.target(), s.source()) && time.mayExecute()) remove(key(s));
        periodic.tick(now, this::present,this::valid);
    }

    public Set<NexusId> persistentPools(UUID target, Set<NexusId> sources) {
        check();
        return group(target).values().stream().filter(s -> sources.contains(s.source())).flatMap(s -> s.plan().ownedShields(s).values().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    /** A derived pool uses its installing behavior's source clock. */
    public NexusId clockSource(UUID target, NexusId source) {
        check();
        for (var value : group(target).values()) if (value.plan().ownedShields(value).containsValue(source)) return value.source();
        return source;
    }
    public Map<NexusId, NexusId> restoredPools(UUID target, List<SavedBehavior> saved) {
        check();
        var result = new HashMap<NexusId, NexusId>();
        for (var value : saved) {
            var source = group(target).get(new Key(target, value.effect(), value.source()));
            if (source != null) { var current = source.plan().ownedShields(source); value.shieldPools().forEach((id, pool) -> { if (current.containsKey(id)) result.put(pool, current.get(id)); }); }
        }
        return Map.copyOf(result);
    }
    public boolean consumed(UUID target,NexusId source) {
        check();return group(target).values().stream().filter(s->s.source().equals(source))
            .anyMatch(s->!protection.available(s, time.now(s.target(), s.source())) && protection.consumed(s.installation()).values().stream().anyMatch(count->count>0));
    }
    public void consumeExternalPeriodic(BehaviorSource source,NexusId contribution) {
        check();CombatMutationGuard.requireWorldWritePhase();
        if(!valid(source))throw new IllegalStateException("Periodic ability source is stale");
        periodic.consumeExternalAbility(source,contribution);
    }
    public List<SavedBehavior> save(UUID target, Set<NexusId> sources, long now) {
        check();
        return group(target).values().stream().filter(s -> sources.contains(s.source())).map(s -> new SavedBehavior(s.effect(), s.source(), s.plan().ownedShields(s),
                protection.consumed(s.installation()), protection.remaining(s.installation(), time.now(s.target(), s.source())), periodic.remaining(s.installation(), time.now(s.target(), s.source())), reactions.persistentCounts(s.installation()))).toList();
    }
    public void restore(UUID target, List<SavedBehavior> saved, long now) {
        check(); CombatMutationGuard.requireWorldWritePhase();
        for (var value : saved) {
            var source = group(target).get(new Key(target, value.effect(), value.source()));
            if (source == null) continue;
            protection.restore(source.installation(), value.consumed(), value.protectionRemaining(), time.now(target, source.source()));
            reactions.restoreCounts(source.installation(), value.triggers());
            periodic.restore(source, value.periodicRemaining(), time.now(target, source.source()));
        }
    }
    public void clear(UUID target) { check(); CombatMutationGuard.requireWorldWritePhase();
        suspended.removeIf(s->s.target().equals(target)); ordered.remove(target); List.copyOf(group(target).keySet()).forEach(this::remove); blocked.removeIf(k -> k.target().equals(target)); }
    public void close() { check(); CombatMutationGuard.requireWorldWritePhase(); active.clear(); suspended.clear(); blocked.clear(); ordered.clear(); periodic.clear(); protection.clear(); closed = true; }
}
