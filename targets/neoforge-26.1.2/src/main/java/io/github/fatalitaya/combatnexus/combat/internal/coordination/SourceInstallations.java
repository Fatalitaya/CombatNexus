package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import io.github.fatalitaya.combatnexus.combat.internal.persistence.SavedSource;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;

import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.StateCommands;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationPlan;
import io.github.fatalitaya.combatnexus.combat.shield.api.*;
import io.github.fatalitaya.combatnexus.effect.api.EffectService;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Shared installation ledger. Failed compensation remains owned and blocks replacement of that instance only. */
public final class SourceInstallations implements SourceService {
    private record Key(UUID target, UUID instance) {}
    private static final class Installed {
        final InstallationHandle handle;
        final SourceInstance source;
        SourcePlan plan;
        Bound definitions;
        boolean pending;
        boolean suspended;
        Installed(InstallationHandle handle, SourceInstance source, SourcePlan plan) { this.handle = handle; this.source = source; this.plan = plan; }
    }
    private final CombatRuntime world;
    private final Map<Key, Installed> installed = new LinkedHashMap<>();
    private final Set<UUID> watched = new LinkedHashSet<>();
    public SourceInstallations(CombatRuntime world) { this.world = world; }
    private final class Bound implements DefinitionBinding {
        final DefinitionBindingSnapshot snapshot;
        final io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration generation;
        Bound(DefinitionBindingSnapshot snapshot, io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration generation) {
            this.snapshot = snapshot; this.generation = generation;
        }
        public DefinitionBindingSnapshot snapshot() { return snapshot; }
        SourceInstallations owner() { return SourceInstallations.this; }
    }
    public DefinitionBinding captureDefinitions() {
        world.requireActive();
        var generation = world.definitions();
        return new Bound(io.github.fatalitaya.combatnexus.combat.internal.persistence.DefinitionBindingValues.capture(generation), generation);
    }
    public DefinitionBinding restoreDefinitions(DefinitionBindingSnapshot snapshot) {
        world.requireActive();
        return new Bound(snapshot, io.github.fatalitaya.combatnexus.combat.internal.persistence.DefinitionBindingValues.compile(snapshot,
            io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions.nextGeneration()));
    }
    private Bound requireBinding(DefinitionBinding definitions) {
        world.requireActive();
        if (!(definitions instanceof SourceInstallations.Bound bound) || bound.owner() != this)
            throw new IllegalArgumentException("Definition binding belongs to another world");
        return bound;
    }
    public io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration definitions(UUID target, SourceInstance source) {
        world.requireActive();
        var value = installed.get(new Key(target, source.instance()));
        return value != null && value.definitions != null ? value.definitions.generation : world.definitions();
    }
    public Optional<io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration> binding(UUID target, NexusId source) {
        world.requireActive();
        return installed.values().stream().filter(i -> i.handle.target().equals(target) && (key(i.source).equals(source)
                || i.plan.regeneration().keySet().stream().anyMatch(id -> part(i.source, id).equals(source))
                || i.plan.shields().keySet().stream().anyMatch(id -> part(i.source, id).equals(source))))
            .filter(i -> i.definitions != null).map(i -> i.definitions.generation).findFirst();
    }
    public SourceResult install(SourceInstallation request, DefinitionBinding definitions) {
        return install(request, null, requireBinding(definitions));
    }
    public SourceService businessView() { return new SourceService() {
        public DefinitionBinding captureDefinitions() { return SourceInstallations.this.captureDefinitions(); }
        public DefinitionBinding restoreDefinitions(DefinitionBindingSnapshot snapshot) { return SourceInstallations.this.restoreDefinitions(snapshot); }
        public SourceResult install(SourceInstallation request, DefinitionBinding definitions) { return SourceInstallations.this.install(request, definitions); }
    public SourceResult install(SourceInstallation request) { return SourceInstallations.this.install(request); }
        public SourceResult install(SourceInstallation request, CombatParent parent) { return SourceInstallations.this.install(request, parent); }
        public CommandResult<Boolean> revoke(UUID request, InstallationHandle handle) { return SourceInstallations.this.revoke(request, handle); }
        public CommandResult<Boolean> revoke(UUID request, InstallationHandle handle, CombatParent parent) { return SourceInstallations.this.revoke(request, handle, parent); }
    public List<SourceView> snapshot(UUID target) { return SourceInstallations.this.snapshot(target); }
        public CommandResult<Boolean> refresh(UUID request, UUID target) { return SourceInstallations.this.refresh(request, target); }
    }; }
            public CommandResult<Boolean> suspend(UUID request,InstallationHandle handle,boolean pause){
        CombatMutationGuard.requireWorldWritePhase();
        var existing=installed.get(new Key(handle.target(),handle.instance()));
        if(existing!=null&&existing.handle==handle&&!existing.pending&&existing.suspended==pause)return CommandResult.applied(true);
        return StateCommands.submit(world,request,handle.target(),CombatOperation.SOURCE,null,()->{
            var value=installed.get(new Key(handle.target(),handle.instance()));
            if(value==null||value.handle!=handle)return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE,"Installation is not current");
            if(value.pending&&!pause)return CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION,"Source installation pending");
            try{setSuspended(value,pause);return CommandResult.applied(true);}
            catch(RuntimeException failure){return CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION,"Suspension reconciliation pending: "+failure.getMessage());}
        });
    }
    private void setSuspended(Installed value,boolean pause){
        var target=value.handle.target();
        // Stop behavior first; it remains stopped if an attribute write needs reconciliation.
        if(pause)world.behaviorControl().suspend(target,key(value.source),true);
        world.effectControl().suspend(target,key(value.source),pause);
        for(var id:value.plan.shields().keySet())world.shieldControl().suspend(target,part(value.source,id),pause);
        for(var id:value.plan.regeneration().keySet())world.regenerationControl().suspend(target,part(value.source,id),pause);
        if(!pause)world.behaviorControl().suspend(target,key(value.source),false);
        value.suspended=pause;
    }
    public void restoreSuspensions(UUID target){
        for(var value:installed.values())if(value.handle.target().equals(target)&&value.suspended)setSuspended(value,true);
    }
    public void track(UUID target) { CombatMutationGuard.requireWorldWritePhase(CombatOperation.SOURCE); if (watched.size() < 4096) watched.add(target); }
    public void tick() {
        if (io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.sourceProviders().isEmpty()) return;
        for (var target : List.copyOf(watched)) {
            if (!world.targetAlive(target)) { watched.remove(target); continue; }
            refresh(UUID.randomUUID(), target);
        }
    }
    public CommandResult<Boolean> refresh(UUID request, UUID target) {
        return StateCommands.submit(world, request, target, CombatOperation.SOURCE, null, () -> {
            track(target);
            boolean complete = true;
            for (var provider : io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.sourceProviders()) {
                Map<UUID, SourceProvider.Offer> offers = new LinkedHashMap<>();
                try (var compute = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.computation()) {
                    var frame = new io.github.fatalitaya.combatnexus.combat.internal.WorldServices(world).facts(target).orElseThrow();
                    var values = List.copyOf(provider.listener().snapshot(frame));
                    if (values.size() > 64) throw new IllegalArgumentException("Source provider capacity exceeded");
                    for (var offer : values) {
                        if (!offer.source().provider().equals(provider.id()) || offers.putIfAbsent(offer.source().instance(), offer) != null)
                            throw new IllegalArgumentException("Foreign or duplicate source offer");
                    }
                } catch (RuntimeException failure) {
                    complete = false;
                    continue;
                }
                for (var value : List.copyOf(installed.values())) if (value.handle.target().equals(target) && value.source.provider().equals(provider.id())) {
                    var desired = offers.get(value.source.instance());
                    if (desired == null || !desired.plan().equals(value.plan) || !desired.source().equals(value.source) || value.pending)
                        complete &= revoke(UUID.randomUUID(), value.handle).applied();
                }
                for (var offer : offers.values()) if (!installed.containsKey(new Key(target, offer.source().instance())))
                    complete &= install(new SourceInstallation(UUID.randomUUID(), target, offer.source(), offer.plan())).applied();
            }
            return complete ? CommandResult.applied(true) : CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Source provider reconciliation incomplete; inspect source views");
        });
    }
    public static NexusId key(SourceInstance source) { return NexusId.fromNamespaceAndPath("combatnexus", "source/" + source.instance()); }
    private static NexusId part(SourceInstance source, NexusId definition) {
        return NexusId.fromNamespaceAndPath("combatnexus", "source/" + source.instance() + "/" + definition.getNamespace() + "/" + definition.getPath());
    }
    public Optional<InstallationHandle> handle(UUID target, UUID instance) { world.requireActive(); return Optional.ofNullable(installed.get(new Key(target, instance))).map(i -> i.handle); }
    public Optional<SourceInstance> identity(UUID target, NexusId key) {
        world.requireActive();
        return installed.values().stream().filter(i -> i.handle.target().equals(target) &&
            (key(i.source).equals(key) || i.plan.regeneration().keySet().stream().anyMatch(id -> part(i.source, id).equals(key))
                || i.plan.shields().keySet().stream().anyMatch(id -> part(i.source, id).equals(key)))).map(i -> i.source).findFirst();
    }
    public boolean active(UUID target,UUID instance){world.requireActive();var value=installed.get(new Key(target,instance));return value!=null&&!value.pending&&!value.suspended;}
    public void refreshAbilities(){
        for(var value:List.copyOf(installed.values()))if(!value.pending){
            try{world.abilityControl().reconcile(value.handle.target(),value.source,value.plan);io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.install(world,value.handle.target(),value.source,value.plan);}
            catch(RuntimeException failure){world.abilityControl().sourceRemoved(value.handle.target(),value.source);io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.remove(world,value.handle.target(),value.source,value.plan);}
        }
    }
    public List<SourceView> snapshot(UUID target) {
        world.requireActive(); return installed.values().stream().filter(i -> i.handle.target().equals(target))
            .map(i -> new SourceView(target, i.source, key(i.source), i.plan.definition(), i.plan.persistent(), i.pending, i.suspended, consumed(i))).toList();
    }
    private boolean consumed(Installed value) {
        var target=value.handle.target();var source=key(value.source);
        if(world.behaviorControl().consumed(target,source))return true;
        if(!value.plan.effects().isEmpty()&&world.effects().snapshot(target).stream().noneMatch(e->e.snapshot().source().equals(source)))return true;
        return !value.plan.shields().isEmpty()&&world.shields().snapshot(target).stream().noneMatch(p->value.plan.shields().keySet().stream().anyMatch(id->part(value.source,id).equals(p.source().id()))&&p.balance()>0);
    }
    /** Update an existing equipment source without reinstalling consumed or expired grants. */
    public CommandResult<Boolean> updateEquipment(UUID request,InstallationHandle handle,SourcePlan plan){
        return StateCommands.submit(world,request,handle.target(),CombatOperation.SOURCE,null,()->{
            var value=installed.get(new Key(handle.target(),handle.instance()));
            if(value==null || value.handle!=handle || !handle.session().equals(world.sessionId()))return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE,"Installation is not current");
            if(value.pending || !value.plan.effects().keySet().equals(plan.effects().keySet()) || !value.plan.shields().keySet().equals(plan.shields().keySet()) || !value.plan.regeneration().keySet().equals(plan.regeneration().keySet()))return CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION,"Source reconciliation pending");
            if(!validAbilities(plan))return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Unknown granted ability");
            try{
                for(var entry:plan.effects().entrySet()){
                    var old=world.effects().snapshot(handle.target()).stream().filter(e->e.snapshot().effect().equals(entry.getKey()) && e.snapshot().source().equals(key(value.source))).findFirst();
                    if(old.isEmpty())continue; // Expired/dispelled/instant grants must not be reissued.
                    var definition=entry.getValue();var before=old.get().snapshot();
                    if(definition.lifetime().kind()==io.github.fatalitaya.combatnexus.effect.api.EffectLifetime.Kind.INSTANT){world.effects().remove(handle.target(),entry.getKey(),key(value.source),EffectService.Removal.REMOVED);continue;}
                    var snapshot=new io.github.fatalitaya.combatnexus.effect.api.EffectSnapshot(before.target(),before.effect(),before.source(),definition.level(),before.expiresAtTick(),definition.restrictions(),before.lifetime());
                    var next=new io.github.fatalitaya.combatnexus.effect.api.EffectInstance(snapshot,definition.attributes(),old.get().ownership(),definition.behavior(),definition.policy(),old.get().hidden(),definition.display(),definition.stacking());
                    if(!world.effects().bind(new io.github.fatalitaya.combatnexus.effect.api.EffectBinding(next,true,EffectService.Stacking.REPLACE.id())))throw new IllegalStateException("Equipment refresh rejected");
                }
                for(var entry:plan.shields().entrySet())world.shieldControl().refresh(handle.target(),part(value.source,entry.getKey()),entry.getValue().definition());
                for(var entry:plan.regeneration().entrySet())world.regenerationControl().refresh(handle.target(),part(value.source,entry.getKey()),entry.getValue());
                world.abilityControl().reconcile(handle.target(),value.source,plan);
                io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.install(world,handle.target(),value.source,plan);
                value.plan=plan;return CommandResult.applied(true);
            }catch(RuntimeException failure){return CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION,"Equipment refresh pending: "+failure.getMessage());}
        });
    }
    private boolean validAbilities(SourcePlan plan){return world.domainDefinitions().abilities().keySet().containsAll(plan.abilities().values());}
    public SourceResult install(SourceInstallation request) { return install(request, (CombatParent) null); }
    public SourceResult install(SourceInstallation request, CombatParent parent) { return install(request, parent, null); }
    private SourceResult install(SourceInstallation request, CombatParent parent, Bound definitions) {
        Objects.requireNonNull(request);
        var attempted = new InstallationHandle[1];
        CommandResult<InstallationHandle> result = StateCommands.submit(world, request.requestId(), request.target(), CombatOperation.SOURCE, Optional.empty(), parent, definitions == null ? null : definitions.generation, () -> {
            var key = new Key(request.target(), request.source().instance());
            if (installed.containsKey(key)) return CommandResult.rejected(CommandRejection.Code.DUPLICATE, "Source instance already installed");
            if (snapshot(request.target()).size() >= 64) return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Source capacity exceeded");
            if(!(definitions == null ? world.domainDefinitions() : definitions.generation.domains()).abilities().keySet().containsAll(request.plan().abilities().values()))return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Unknown granted ability");
            var value = new Installed(new InstallationHandle(world.sessionId(), request.target(), request.source().instance()), request.source(), request.plan());
            value.definitions = definitions;
            installed.put(key, value); attempted[0] = value.handle;
            try {
                long now = world.businessTick(request.target(), key(value.source));
                for (var entry : value.plan.effects().entrySet()) {
                    var definition = entry.getValue();
                    if (!world.effects().apply(definition.instantiate(request.target(), entry.getKey(), key(value.source), now), definition.stacking()))
                        throw new IllegalStateException("Source effect rejected: " + entry.getKey());
                }
                for (var entry : value.plan.regeneration().entrySet()) {
                    var source = new CombatSource(part(value.source, entry.getKey()), value.source.owner(), Optional.empty());
                    world.regeneration().replace(new RegenerationPlan(request.target(), source, entry.getValue(), OptionalLong.of(Math.addExact(world.businessTick(request.target(), source.id()), entry.getValue().durationTicks()))));
                }
                for (var entry : value.plan.shields().entrySet()) {
                    var grant = entry.getValue();
                    world.shields().replace(request.target(), new ShieldGrant(new CombatSource(part(value.source, entry.getKey()), value.source.owner(), Optional.empty()),
                        grant.definition(), grant.amount(), OptionalLong.of(Math.addExact(world.businessTick(request.target(), part(value.source, entry.getKey())), grant.durationTicks()))));
                }
                world.abilityControl().reconcile(request.target(),value.source,value.plan);
                io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.install(world,request.target(),value.source,value.plan);
                return CommandResult.applied(value.handle);
            } catch (RuntimeException failure) {
                value.pending = true;
                boolean confirmedEmpty=value.plan.shields().isEmpty()&&value.plan.regeneration().isEmpty()
                    &&value.plan.effects().values().stream().noneMatch(e->e.lifetime().kind()==io.github.fatalitaya.combatnexus.effect.api.EffectLifetime.Kind.INSTANT)
                    &&world.effects().snapshot(request.target()).stream().noneMatch(e->e.snapshot().source().equals(key(value.source)))
                    &&!world.effects().pendingSources(request.target()).contains(key(value.source));
                if(confirmedEmpty)installed.remove(key);
                return CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Source installation incomplete; confirmed writes and source ownership retained");
            }
        });
        var retained = result.value();
        if (retained.isEmpty() && attempted[0] != null && installed.containsKey(new Key(attempted[0].target(), attempted[0].instance()))) retained = Optional.of(attempted[0]);
        return new SourceResult(retained, result.rejection(), result.journal(), result.context());
    }
    public CommandResult<Boolean> revoke(UUID request, InstallationHandle handle) { return revoke(request, handle, null); }
    public CommandResult<Boolean> revoke(UUID request, InstallationHandle handle, CombatParent parent) {
        Objects.requireNonNull(handle);
        return StateCommands.submit(world, request, handle.target(), CombatOperation.SOURCE, parent, () -> {
            var key = new Key(handle.target(), handle.instance()); var value = installed.get(key);
            if (value == null || value.handle != handle || !handle.session().equals(world.sessionId()))
                return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE, "Installation handle is not current");
            try { cleanup(value, false); installed.remove(key); return CommandResult.applied(true); }
            catch (RuntimeException failure) {
                value.pending = true;
                return CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Source cleanup pending");
            }
        });
    }
    private void cleanup(Installed value, boolean rollback) {
        var target = value.handle.target();
        world.abilityControl().sourceRemoved(target,value.source);
        io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.remove(world,target,value.source,value.plan);
        world.driverControl().sourceRemoved(target,key(value.source));
        world.timeControl().sourceRemoved(target,key(value.source));
        world.participantStates().sourceRemoved(target, value.source.instance());
        for (var id : value.plan.regeneration().keySet()) world.regeneration().remove(target, part(value.source, id));
        for (var id : value.plan.effects().keySet()) world.effects().remove(target, id, key(value.source), EffectService.Removal.REMOVED);
        world.effectControl().suspend(target,key(value.source),false);
        world.behaviorControl().suspend(target,key(value.source),false);
        for (var entry : value.plan.shields().entrySet()) if (rollback || entry.getValue().removal() == SourcePlan.ShieldRemoval.REVOKE)
            world.shields().remove(target, part(value.source, entry.getKey()), ShieldPoolSnapshot.RemovalReason.REMOVED);
    }
    public void clear(UUID target) { installed.keySet().removeIf(key -> key.target().equals(target)); watched.remove(target); }
    public void close() { installed.clear(); watched.clear(); }

    public List<SavedSource> save(UUID target) {
        return installed.values().stream().filter(i -> i.handle.target().equals(target) && i.plan.persistent()).map(i -> new SavedSource(i.source, i.plan, i.suspended, i.pending, i.definitions == null ? null : i.definitions.snapshot.semanticIdentity())).toList();
    }
    public Set<NexusId> persistentKeys(UUID target) {
        var keys = new HashSet<NexusId>();
        for (var saved : save(target)) {
            keys.add(key(saved.source()));
            saved.plan().regeneration().keySet().forEach(id -> keys.add(part(saved.source(), id)));
            saved.plan().shields().keySet().forEach(id -> keys.add(part(saved.source(), id)));
        }
        return Set.copyOf(keys);
    }
    public Map<UUID, DefinitionBindingSnapshot> saveBindings(UUID target) {
        var result = new LinkedHashMap<UUID, DefinitionBindingSnapshot>();
        for (var installed : installed.values()) if (installed.handle.target().equals(target) && installed.plan.persistent() && installed.definitions != null) {
            var binding = installed.definitions.snapshot;
            var previous = result.putIfAbsent(binding.semanticIdentity(), binding);
            if (previous != null && !previous.equals(binding)) throw new IllegalStateException("Conflicting definition binding values");
        }
        return Map.copyOf(result);
    }
    public void restore(UUID target, List<SavedSource> saved, Map<UUID, DefinitionBindingSnapshot> values) {
        var bindings = new LinkedHashMap<UUID, Bound>();
        values.forEach((id, snapshot) -> bindings.put(id, requireBinding(restoreDefinitions(snapshot))));
        var prepared = new LinkedHashMap<Key, Installed>();
        for (var value : saved) {
            var key = new Key(target, value.source().instance());
            var restored=new Installed(new InstallationHandle(world.sessionId(), target, value.source().instance()), value.source(), value.plan());
            restored.definitions = value.binding() == null ? null : Objects.requireNonNull(bindings.get(value.binding()), "Missing saved definition binding");
            restored.suspended=value.suspended();restored.pending=value.pending();
            if (installed.containsKey(key) || prepared.putIfAbsent(key, restored) != null)
                throw new IllegalArgumentException("Duplicate saved source");
        }
        installed.putAll(prepared);
    }
    public void notifyRestored(UUID target) {
        for(var value:installed.values())if(value.handle.target().equals(target)&&!value.pending)world.abilityControl().reconcile(target,value.source,value.plan);
        for (var value : List.copyOf(installed.values())) if (value.handle.target().equals(target)) {
            var view = new SourceView(target, value.source, key(value.source), value.plan.definition(), value.plan.persistent(), value.pending, value.suspended, consumed(value));
            io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.restoredSource(world.serverSession(),
                    new RestoredSource(view, value.handle));
        }
    }
}
