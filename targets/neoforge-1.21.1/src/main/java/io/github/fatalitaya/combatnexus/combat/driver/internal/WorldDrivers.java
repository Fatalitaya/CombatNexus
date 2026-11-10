package io.github.fatalitaya.combatnexus.combat.driver.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.driver.api.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.fatalitaya.combatnexus.combat.time.api.TimeAdvanceReceipt;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.entity.LivingEntity;

/** World-owned authority. Saved ownership and public snapshots cannot open execution scopes. */
public final class WorldDrivers implements DriverService {
    private record Key(UUID target, Optional<NexusId> source, DriverScope scope) {}
    private static final class Claim {
        final ParticipantIdentity target;
        final NexusId driver;
        final Set<DriverScope> scopes;
        final Optional<NexusId> source;
        Session session;
        Claim(ParticipantIdentity target, NexusId driver, Set<DriverScope> scopes, Optional<NexusId> source) {
            this.target=target; this.driver=driver; this.scopes=Set.copyOf(scopes); this.source=source;
        }
    }
    private record Execution(Session session, DriverScope scope) {}
    private final CombatRuntime world;
    private final Map<Key, Claim> claims = new LinkedHashMap<>();
    private final Deque<Execution> active = new ArrayDeque<>();
    public WorldDrivers(CombatRuntime world) { this.world=world; }
    private void writable() { world.requireActive(); CombatMutationGuard.requireWorldWritePhase(); CombatExecutionScope.requireSubmissionPhase(); }
    public DriverService businessView() { return new DriverService() {
        public DriverSession bind(DriverRegistration registration, UUID target, Set<DriverScope> scopes, Optional<NexusId> source) { return WorldDrivers.this.bind(registration,target,scopes,source); }
        public List<DriverView> snapshot(UUID target) { return WorldDrivers.this.snapshot(target); }
    }; }
    @Override public DriverSession bind(DriverRegistration registration, UUID target, Set<DriverScope> requested, Optional<NexusId> source) {
        writable(); var definition=DriverRegistrations.require(registration); var scopes=Set.copyOf(requested); Objects.requireNonNull(source);
        if(source.isPresent() && scopes.contains(DriverScope.NUMERICAL))throw new IllegalArgumentException("Numerical ownership requires an entity range");
        if(scopes.isEmpty() || !definition.scopes().containsAll(scopes))throw new IllegalArgumentException("Driver scope not declared");
        var identity=world.participantIdentity(target).orElseThrow(()->new IllegalArgumentException("Driver target unavailable"));
        if(!world.targetAlive(target))throw new IllegalArgumentException("Driver target must be alive");
        if(source.isPresent() && world.sourceInstallations().identity(target,source.get()).filter(value->io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations.key(value).equals(source.get())).isEmpty())throw new IllegalArgumentException("Driver source is not installed");
        Claim existing=null;
        for(var scope:scopes) {
            var found=claims.get(new Key(target,source,scope));
            if(found==null)continue;
            if(!found.driver.equals(definition.id()) || !found.target.equals(identity) || !found.scopes.equals(scopes) || existing!=null&&found!=existing)
                throw new IllegalStateException("Driver scope already owned");
            existing=found;
        }
        if(existing!=null && existing.session!=null) { existing.session.check(); return existing.session; }
        if(existing==null && (claims.size()+scopes.size()>4096 || snapshot(target).size()>=64))throw new IllegalStateException("Driver session capacity");
        var claim=existing!=null?existing:new Claim(identity,definition.id(),scopes,source);
        if(scopes.contains(DriverScope.TIME))world.timeControl().acquire(target,source,definition.id());
        var session=new Session(claim,definition);claim.session=session;
        for(var scope:scopes)claims.put(new Key(target,source,scope),claim);
        return session;
    }
    @Override public List<DriverView> snapshot(UUID target) {
        world.requireActive(); return claims.values().stream().distinct().filter(c->c.target.entity().equals(target)).map(this::view).toList();
    }
    private DriverView view(Claim claim) { return new DriverView(claim.target,claim.driver,claim.scopes,claim.source,claim.session!=null&&!claim.session.closed); }
    private Claim effective(UUID target, DriverScope scope, NexusId source) {
        var specific=source==null?null:claims.get(new Key(target,Optional.of(source),scope));
        return specific!=null?specific:claims.get(new Key(target,Optional.empty(),scope));
    }
    public boolean allowsAutomatic(UUID target, DriverScope scope, NexusId source) {
        world.requireActive(); var claim=effective(target,scope,source); if(claim==null)return true;
        if(!world.participantIdentity(target).filter(claim.target::equals).isPresent())return false;
        var execution=active.peekLast();
        return execution!=null && execution.session.claim==claim && execution.scope==scope && !execution.session.closed;
    }
    public List<SavedDriver> save(UUID target) {
        world.requireActive(); return claims.values().stream().distinct().filter(c->c.target.entity().equals(target))
            .map(c->new SavedDriver(c.driver,c.scopes,c.source.orElse(null))).toList();
    }
    public void restore(UUID target,List<SavedDriver> saved) {
        writable(); if(saved.size()>64)throw new IllegalArgumentException("Saved driver capacity");
        var identity=world.participantIdentity(target).orElseThrow();var next=new LinkedHashMap<Key,Claim>();
        for(var value:saved) {
            var claim=new Claim(identity,value.driver(),value.scopes(),Optional.ofNullable(value.source()));
            for(var scope:claim.scopes) {
                var key=new Key(target,claim.source,scope);
                if(claim.scopes.isEmpty() || claims.containsKey(key) || next.putIfAbsent(key,claim)!=null)throw new IllegalArgumentException("Duplicate saved driver ownership");
            }
        }
        if(claims.size()+next.size()>4096)throw new IllegalArgumentException("Saved driver capacity");
        claims.putAll(next);
    }
    public void sourceRemoved(UUID target,NexusId source) {
        writable(); for(var claim:List.copyOf(new LinkedHashSet<>(claims.values())))if(claim.target.entity().equals(target)&&claim.source.equals(Optional.of(source)))remove(claim,false);
    }
    public void clear(UUID target) {
        world.requireActive();for(var claim:List.copyOf(new LinkedHashSet<>(claims.values())))if(claim.target.entity().equals(target))remove(claim,false);
    }
    public void close() { for(var claim:claims.values())if(claim.session!=null)claim.session.closed=true;claims.clear();active.clear(); }
    private void remove(Claim claim,boolean releaseTime) {
        if(releaseTime&&claim.scopes.contains(DriverScope.TIME))world.timeControl().release(claim.target.entity(),claim.source,claim.driver);
        if(claim.session!=null)claim.session.closed=true;
        claims.entrySet().removeIf(e->e.getValue()==claim);
    }
    /** The exact native request is marked only after external calculation; nested ordinary requests cannot inherit it. */
    public <T> T resolveDamage(UUID requestId, DamageRequest request,
            io.github.fatalitaya.combatnexus.combat.api.CombatSource source, Supplier<T> application) {
        world.requireActive();
        io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.requireSubmissionPhase();
        var claim = effective(request.target().getUUID(), DriverScope.NUMERICAL, source.id());
        if (claim == null || claim.session == null || claim.session.resolver == null) return application.get();
        var session = claim.session;
        session.require(DriverScope.NUMERICAL);
        ResolvedDamage resolved;
        try (var read = io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
            var input = new NumericalResolver.Damage(requestId, claim.target, source,
                request.source().damageType(), request.baseDamage());
            resolved = new ResolvedDamage(requestId, source, input.damageType(), session.resolver.damage(input));
        }
        session.check();
        for (var component : resolved.components())
            if (!world.sourceInstallations().binding(source.owner().orElse(request.target().getUUID()), source.id()).orElseGet(world::definitions).damage().channels().channel(component.channel()).id().equals(component.channel()))
                throw new IllegalArgumentException("Unknown resolved channel: " + component.channel());
        try (var numbers = ResolvedNumbers.damage(request, resolved, session.definition)) { return application.get(); }
    }

    public HealingSettlement resolveHealing(HealingRequest request) {
        world.requireActive();
        io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.requireSubmissionPhase();
        var claim = effective(request.target(), DriverScope.NUMERICAL, request.source().id());
        if (claim == null || claim.session == null || claim.session.resolver == null) return world.healing().submit(request);
        var session = claim.session;
        session.require(DriverScope.NUMERICAL);
        if (request.targetInstance().isPresent() && !request.targetInstance().get().equals(claim.target))
            throw new IllegalArgumentException("Healing target instance changed");
        double amount;
        try (var read = io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
            amount = session.resolver.healing(new NumericalResolver.Healing(request, claim.target));
        }
        session.check();
        var resolved = new HealingRequest(request.requestId(), request.target(), request.source(), request.reason(),
            amount, request.healer(), Optional.of(claim.target));
        try (var numbers = ResolvedNumbers.healing(resolved)) { return world.healing().submit(resolved); }
    }

    private final class Session implements DriverSession {
        final Claim claim; final DriverDefinition definition; boolean closed;
        NumericalResolver resolver;
        Session(Claim claim,DriverDefinition definition){this.claim=claim;this.definition=definition;}
        void check() {
            world.requireActive();
            if(closed || claim.session!=this || !world.participantIdentity(claim.target.entity()).filter(claim.target::equals).isPresent())throw new IllegalStateException("Driver session is stale");
            for(var scope:claim.scopes)if(claims.get(new Key(claim.target.entity(),claim.source,scope))!=claim)throw new IllegalStateException("Driver ownership revoked");
        }
        void require(DriverScope scope) { check(); if(!claim.scopes.contains(scope))throw new IllegalStateException("Driver session does not own " + scope); }
        void requireSource(NexusId source) { if(claim.source.isPresent()&&!claim.source.get().equals(source))throw new IllegalArgumentException("Command belongs to another driver source"); }
        public DriverView snapshot(){check();return view(claim);}
        public <T> T withNumericalResolver(NumericalResolver resolver, Supplier<T> execution) {
            writable(); require(DriverScope.NUMERICAL);
            Objects.requireNonNull(resolver); Objects.requireNonNull(execution);
            if (this.resolver != null) throw new IllegalStateException("Numerical resolver already active");
            this.resolver = resolver;
            try { return execution.get(); } finally { this.resolver = null; }
        }
        public <T> T withExecution(DriverScope scope,Supplier<T> execution) {
            writable();require(scope);Objects.requireNonNull(execution);
            if(scope==DriverScope.TIME||scope==DriverScope.NUMERICAL)throw new IllegalArgumentException("Use the typed time or resolved numerical entry point");
            if(active.size()>=64)throw new IllegalStateException("Driver execution depth");
            var entry=new Execution(this,scope);active.addLast(entry);
            try{return execution.get();}finally{if(active.peekLast()!=entry)throw new IllegalStateException("Driver execution scope mismatch");active.removeLast();}
        }
        public io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult execute(io.github.fatalitaya.combatnexus.combat.ability.api.AbilityRequest request) {
            require(DriverScope.ACTIVE);requireSource(io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations.key(request.grant().source()));
            if(!request.grant().actor().equals(claim.target))throw new IllegalArgumentException("Ability belongs to another driver actor");
            return withExecution(DriverScope.ACTIVE,()->world.abilities().execute(request));
        }
        public io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult trigger(io.github.fatalitaya.combatnexus.combat.ability.api.AbilityRequest request) {
            check();requireSource(io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations.key(request.grant().source()));
            if(!request.grant().actor().equals(claim.target))throw new IllegalArgumentException("Ability belongs to another driver actor");
            var definition=world.abilityControl().definition(request.grant());
            if(definition==null)throw new IllegalArgumentException("Ability definition unavailable");
            var scope=definition.trigger().kind()==io.github.fatalitaya.combatnexus.combat.ability.api.AbilityTrigger.Kind.PERIODIC?DriverScope.PERIODIC:DriverScope.TRIGGER;
            return withExecution(scope,()->world.abilityControl().externalTrigger(request));
        }
        public io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult resumeAbility(UUID execution) {
            check();var snapshot=world.abilities().execution(execution).orElseThrow(()->new IllegalArgumentException("Ability execution unavailable"));
            if(!snapshot.grant().actor().equals(claim.target))throw new IllegalArgumentException("Ability belongs to another driver actor");
            requireSource(io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations.key(snapshot.grant().source()));
            return withExecution(world.abilityControl().executionScope(execution),()->world.abilityControl().resumeExternal(claim.target.entity(),execution));
        }
        public io.github.fatalitaya.combatnexus.combat.time.api.TimeStatus time() { require(DriverScope.TIME);return world.timeControl().status(claim.target.entity(),claim.source,claim.driver); }
        public TimeAdvanceReceipt advance(long sequence,UUID request,long ticks) { writable();require(DriverScope.TIME);return world.timeControl().advance(claim.target.entity(),claim.source,claim.driver,sequence,request,ticks); }
        public TimeAdvanceReceipt cancelAdvance(long sequence,UUID request,long ticks) { writable();require(DriverScope.TIME);return world.timeControl().cancel(claim.target.entity(),claim.source,claim.driver,sequence,request,ticks); }
        public Optional<UUID> applyingDamage(LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
            require(DriverScope.NUMERICAL);
            if (world.level().getEntity(claim.target.entity()) != target) return Optional.empty();
            return ResolvedNumbers.applying(definition, target, source);
        }
        public DamageResult damage(ResolvedDamage request) {
            require(DriverScope.NUMERICAL);
            var target=(LivingEntity)world.level().getEntity(claim.target.entity());
            var builder=DamageRequest.builder(world.level(),target,DamageSourceDescriptor.of(request.damageType()),request.amount()).kind(DamageRequestKind.CUSTOM);
            request.source().owner().map(world.level()::getEntity).ifPresent(owner -> {
                builder.effectOwner(owner).directEntity(owner);
                if (owner instanceof LivingEntity living) builder.logicalAttacker(living);
            });
            return damage(request, builder.build());
        }
        public DamageResult damage(ResolvedDamage request, DamageRequest nativeRequest) {
            writable();require(DriverScope.NUMERICAL);requireSource(request.source().id());
            if(effective(claim.target.entity(),DriverScope.NUMERICAL,request.source().id())!=claim)throw new IllegalStateException("Numerical source has a different execution owner");
            if (nativeRequest.level() != world.level()
                    || nativeRequest.target() != world.level().getEntity(claim.target.entity())
                    || !nativeRequest.source().damageType().equals(request.damageType())
                    || Float.compare(nativeRequest.baseDamage(), request.amount()) != 0)
                throw new IllegalArgumentException("Resolved values differ from native request");
            if (!java.util.Optional.ofNullable(nativeRequest.effectOwner()).map(net.minecraft.world.entity.Entity::getUUID).equals(request.source().owner()))
                throw new IllegalArgumentException("Resolved source owner differs from native attribution");
            if(!world.serverRuntime().claim(CombatOperation.DAMAGE,request.requestId()))return DamageResult.rejected(nativeRequest,DamageFailureReason.DUPLICATE_REQUEST,"Resolved request already consumed");
            for(var component:request.components())if(!world.sourceInstallations().binding(request.source().owner().orElse(claim.target.entity()), request.source().id()).orElseGet(world::definitions).damage().channels().channel(component.channel()).id().equals(component.channel()))throw new IllegalArgumentException("Unknown resolved channel: "+component.channel());
            try(var numbers=ResolvedNumbers.damage(nativeRequest,request,definition)) { return io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleApi.submitDamage(nativeRequest); }
        }
        public HealingSettlement heal(HealingRequest request) {
            writable();require(DriverScope.NUMERICAL);requireSource(request.source().id());
            if(effective(claim.target.entity(),DriverScope.NUMERICAL,request.source().id())!=claim)throw new IllegalStateException("Numerical source has a different execution owner");
            if(!request.target().equals(claim.target.entity()) || request.targetInstance().isPresent()&&!request.targetInstance().get().equals(claim.target))throw new IllegalArgumentException("Healing belongs to another driver target");
            try(var numbers=ResolvedNumbers.healing(request)) { return world.healing().submit(request); }
        }
        public boolean released() { return closed; }
        public ReleaseResult release() {
            if (closed) return ReleaseResult.RELEASED;
            writable(); check();
            if (resolver != null || active.stream().anyMatch(e -> e.session == this)) return ReleaseResult.EXECUTING;
            if (claim.scopes.contains(DriverScope.TIME)
                    && time().latest().filter(receipt -> !receipt.terminal()).isPresent())
                return ReleaseResult.PENDING_TIME;
            remove(claim, true);
            return ReleaseResult.RELEASED;
        }
        public void close() {
            var result = release();
            if (result != ReleaseResult.RELEASED)
                throw new IllegalStateException("Driver release pending: " + result);
        }
    }
}
