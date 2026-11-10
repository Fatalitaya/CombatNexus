package io.github.fatalitaya.combatnexus.combat.internal.registration;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatListenerRegistry;

import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.pipeline.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldSettlement;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.ability.api.AbilityFact;
import java.util.function.Consumer;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Immutable extension tables shared by world sessions, built at loader startup. */
public final class CombatExtensions {
    private static final Object OWNER = new Object();
    private static boolean initialized;
    private static final io.github.fatalitaya.combatnexus.combat.internal.definition.ItemBindingDefinitions ITEM_BINDINGS=new io.github.fatalitaya.combatnexus.combat.internal.definition.ItemBindingDefinitions();
    public static java.util.Map<NexusId,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec> itemBindings(java.util.List<io.github.fatalitaya.combatnexus.combat.internal.definition.ItemBindingDefinitions.Resource> resources,java.util.Map<NexusId,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec> defaults){return ITEM_BINDINGS.compile(resources,defaults);}
    private static final io.github.fatalitaya.combatnexus.combat.internal.definition.JavaCombatDefinitions DEFINITIONS = new io.github.fatalitaya.combatnexus.combat.internal.definition.JavaCombatDefinitions();
    public static io.github.fatalitaya.combatnexus.config.DefinitionResources definitionDefaults(io.github.fatalitaya.combatnexus.config.DefinitionResources packs) { return DEFINITIONS.overlay(packs); }
    private static List<io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy> settlementPolicies=List.of();
    public static List<io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy> settlementPolicies(){return settlementPolicies;}
    private static Map<NexusId,Consumer<DefinitionView>> definitionObservers=Map.of();
    public static void definitions(ServerSession server,DefinitionView view) {
        definitionObservers.forEach((id,observer)->{try(var scope=CombatDispatchScope.observation(OWNER,view,server)){observer.accept(view);}catch(Exception error){CombatFailures.rethrowFatal(error);}});
    }
    private record MomentObserver<T extends io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment>(NexusId id,Class<T> type,Consumer<T> observer) {
        void deliver(ServerSession server,io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment moment) {
            if(!type.isInstance(moment))return;
            try(var scope=CombatDispatchScope.observation(OWNER,moment,server)) { observer.accept(type.cast(moment)); }
            catch(Exception error) { CombatFailures.rethrowFatal(error); }
        }
    }
    private static List<MomentObserver<?>> moments=List.of();
    public static void moment(net.minecraft.server.level.ServerLevel level,io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment moment) {
        if(moments.stream().noneMatch(m->m.type().isInstance(moment)))return;
        var kernel=io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer()));
        if(kernel.isEmpty())return;
        kernel.get().rejectionCompletion().ifPresent(slot->{try(slot){slot.publish(()->moments.forEach(observer->observer.deliver(kernel.get().session(),moment)));}});
    }
    private static Map<NexusId,java.util.function.Function<io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame,List<io.github.fatalitaya.combatnexus.combat.api.source.SourceDescription>>> descriptions=Map.of();
    public static List<io.github.fatalitaya.combatnexus.combat.api.source.SourceDescription> describe(io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame frame) {
        var result=new ArrayList<io.github.fatalitaya.combatnexus.combat.api.source.SourceDescription>();
        descriptions.forEach((id,provider)->{
            try(var read=CombatReadScope.open()) { var entries=List.copyOf(provider.apply(frame)); if(entries.size()>128)throw new IllegalArgumentException("Source description capacity");result.addAll(entries); }
            catch(Exception failure) { CombatFailures.rethrowFatal(failure); }
        });
        return List.copyOf(result);
    }
    private static Map<NexusId, io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy> damageFormulas = Map.of();
    public static io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy damageFormula(NexusId id) {
        return Objects.requireNonNull(damageFormulas.get(id), "Unregistered damage formula: " + id);
    }
    private static Map<NexusId, Consumer<io.github.fatalitaya.combatnexus.combat.api.source.RestoredSource>> restorationOwners = Map.of();
    public static void restoredSource(ServerSession server, io.github.fatalitaya.combatnexus.combat.api.source.RestoredSource restored) {
        var owner = restorationOwners.get(restored.source().source().provider());
        if (owner == null) return;
        try (var observation = CombatDispatchScope.observation(OWNER, restored, server)) {
            owner.accept(restored);
        } catch (Exception exception) { CombatFailures.rethrowFatal(exception); }
    }
    private record Collector<T>(io.github.fatalitaya.combatnexus.combat.api.input.FactKey<T> key,
            java.util.function.Function<net.minecraft.world.entity.LivingEntity, T> capture) {
        String encode(net.minecraft.world.entity.LivingEntity entity) {
            return key.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, capture.apply(entity)).getOrThrow().toString();
        }
    }
    private static List<Collector<?>> collectors = List.of();
    public static io.github.fatalitaya.combatnexus.combat.api.input.FrozenFacts restoreFacts(
            Map<NexusId, String> encoded, Set<NexusId> incomplete) {
        var values = new LinkedHashMap<io.github.fatalitaya.combatnexus.combat.api.input.FactKey<?>, String>();
        for (var entry : encoded.entrySet()) {
            var key = collectors.stream().filter(collector -> collector.key().id().equals(entry.getKey()))
                    .map(Collector::key).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown saved fact: " + entry.getKey()));
            values.put(key, entry.getValue());
        }
        return new io.github.fatalitaya.combatnexus.combat.api.input.FrozenFacts(values, incomplete);
    }
    public static io.github.fatalitaya.combatnexus.combat.api.input.FrozenFacts captureFacts(net.minecraft.world.entity.LivingEntity entity) {
        var values = new LinkedHashMap<io.github.fatalitaya.combatnexus.combat.api.input.FactKey<?>, String>();
        var incomplete = new HashSet<NexusId>();
        for (var collector : collectors) {
            try (var read = io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
                values.put(collector.key(), collector.encode(entity));
            }
            catch (Exception exception) {
                incomplete.add(collector.key().id());
                CombatFailures.rethrowFatal(exception);
            }
        }
        return new io.github.fatalitaya.combatnexus.combat.api.input.FrozenFacts(values, incomplete);
    }
    private static List<io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCapability> bridges = List.of();
    public static List<io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCapability> bridgeCoverage() { return bridges; }
    private static List<CombatListenerRegistry.Entry<io.github.fatalitaya.combatnexus.combat.api.source.SourceProvider>> providers = List.of();
    public static List<CombatListenerRegistry.Entry<io.github.fatalitaya.combatnexus.combat.api.source.SourceProvider>> sourceProviders() { return providers; }
    private static DomainPipeline<HealingPhase, Consumer<HealingCalculation>> healing = new DomainPipeline.Builder<HealingPhase, Consumer<HealingCalculation>>(HealingPhase.class).freeze();
    private record AbilityReaction(NexusId id,int priority,Set<CombatOperation> permissions,Consumer<CombatCallback<AbilityFact>> handler) {}
    private static List<AbilityReaction> abilityReactions=List.of();
    private static List<CombatListenerRegistry.Entry<Consumer<AbilityFact>>> abilityObservers=List.of();
    private static List<Reaction> healingReactions = List.of();
    private static List<CombatListenerRegistry.Entry<Consumer<HealingSettlement>>> healingObservers = List.of();
    private static List<CombatListenerRegistry.Entry<Consumer<ShieldSettlement>>> shieldObservers = List.of();
    private static List<CombatListenerRegistry.Entry<Consumer<CommandResult<?>>>> stateObservers = List.of();
    private record Reaction(NexusId id, int priority, Set<CombatOperation> permissions, Consumer<CombatCallback<HealingSettlement>> handler) {}
    private CombatExtensions() {}
    public static synchronized void initialize(Consumer<CombatRegistrar> registration) {
        if (initialized) throw new IllegalStateException("Combat extensions already registered");
        IntentTypes.builtins();
        io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.register();
        IntentTypes.register(new io.github.fatalitaya.combatnexus.combat.api.scheduling.IntentType<>(new NexusId("combatnexus","ability_continuation"),io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityContinuationIntent.class,io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityContinuationIntent.CODEC,Set.of(CombatOperation.ABILITY),(value,context)->{throw new IllegalStateException("Ability continuation requires its scheduled execution owner");}));
        io.github.fatalitaya.combatnexus.effect.internal.BuiltinEffectBehaviorDescriptors.register();
        var session = new Session();
        session.settlementPolicy(io.github.fatalitaya.combatnexus.combat.internal.coordination.LifestealRequests.policy());
        try { registration.accept(session); }
        finally { session.open = false; DEFINITIONS.freeze(); ITEM_BINDINGS.freeze(); }
        var validatedBridges = session.bridges.freeze();
        healing = session.pipeline.freeze();
        session.providers.freeze(); providers = session.providers.entries();
        session.healObservers.freeze(); healingObservers = session.healObservers.entries();
        session.shieldObservers.freeze(); shieldObservers = session.shieldObservers.entries();
        session.abilityObservers.freeze();abilityObservers=session.abilityObservers.entries();
        abilityReactions=session.abilityReactions.values().stream().sorted(Comparator.comparingInt(AbilityReaction::priority).reversed().thenComparing(r->r.id().toString())).toList();
        session.stateObservers.freeze(); stateObservers = session.stateObservers.entries();
        healingReactions = session.reactions.values().stream().sorted(Comparator.comparingInt(Reaction::priority).reversed().thenComparing(r -> r.id().toString())).toList();
        BehaviorTypes.freeze();
        io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageDescriptions.registration().freeze();
        io.github.fatalitaya.combatnexus.effect.internal.EffectBehaviorDescriptors.freeze();
        io.github.fatalitaya.combatnexus.effect.internal.EffectCapabilities.freeze();
        io.github.fatalitaya.combatnexus.effect.internal.EffectStackingStrategies.freeze();
        io.github.fatalitaya.combatnexus.effect.internal.EnvironmentEffects.freeze();
        io.github.fatalitaya.combatnexus.effect.internal.EffectMappings.freeze();
        IntentTypes.freeze();
        io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityExecutors.freeze();
        io.github.fatalitaya.combatnexus.combat.driver.internal.DriverRegistrations.freeze();
        settlementPolicies=session.settlementPolicies.values().stream().sorted(Comparator.comparingInt(io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy::priority).reversed().thenComparing(v->v.id().toString())).toList();
        io.github.fatalitaya.combatnexus.combat.state.internal.RuntimeStateTypes.install(session.stateTypes);
        bridges = validatedBridges;
        damageFormulas = Map.copyOf(session.damageFormulas);
        restorationOwners = Map.copyOf(session.restorationOwners);
        collectors = List.copyOf(session.collectors.values());
        moments=List.copyOf(session.moments.values());
        descriptions=Collections.unmodifiableMap(new LinkedHashMap<>(session.descriptions));
        definitionObservers=Collections.unmodifiableMap(new LinkedHashMap<>(session.definitionObservers));
        nativeIngress = List.copyOf(session.nativeIngress.values());
        damageBodies = List.copyOf(session.damageBodies.values());
        initialized = true;
    }
    private static java.util.List<java.util.function.Function<net.minecraft.world.entity.Entity,net.minecraft.world.entity.LivingEntity>> damageBodies = List.of();
    public static net.minecraft.world.entity.LivingEntity damageBodyOwner(net.minecraft.world.entity.Entity body) {
        net.minecraft.world.entity.LivingEntity selected = null;
        for (var resolver : damageBodies) {
            var owner = resolver.apply(body);
            if (owner == null) continue;
            if (selected != null) throw new IllegalStateException("Conflicting damage body owners");
            selected = owner;
        }
        return selected;
    }
    private static java.util.List<io.github.fatalitaya.combatnexus.combat.api.platform.NativeDamageIngress> nativeIngress = java.util.List.of();
    public static boolean replaceNativeDamage(net.minecraft.world.entity.LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount) {
        for (var ingress : nativeIngress) if (ingress.replace(target, source, amount)) return true;
        return false;
    }
    private static final class Session implements CombatRegistrar, HealingRegistrar {
        final Map<NexusId,java.util.function.Function<net.minecraft.world.entity.Entity,net.minecraft.world.entity.LivingEntity>> damageBodies = new LinkedHashMap<>();
        public void damageBody(NexusId id, java.util.function.Function<net.minecraft.world.entity.Entity,net.minecraft.world.entity.LivingEntity> owner) {
            check(); Objects.requireNonNull(id); Objects.requireNonNull(owner);
            if (damageBodies.putIfAbsent(id, owner) != null) throw new IllegalArgumentException("Duplicate damage body resolver");
        }

        final Map<NexusId,io.github.fatalitaya.combatnexus.combat.api.platform.NativeDamageIngress> nativeIngress = new LinkedHashMap<>();
        public void nativeDamageIngress(NexusId id, io.github.fatalitaya.combatnexus.combat.api.platform.NativeDamageIngress ingress) {
            check(); Objects.requireNonNull(id); Objects.requireNonNull(ingress);
            if (nativeIngress.putIfAbsent(id, ingress) != null) throw new IllegalArgumentException("Duplicate native damage ingress");
        }

        public void abilityExecutor(NexusId id,io.github.fatalitaya.combatnexus.combat.ability.api.AbilityExecutor executor){check();io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityExecutors.register(id,executor);}
        public io.github.fatalitaya.combatnexus.combat.driver.api.DriverRegistration externalDriver(io.github.fatalitaya.combatnexus.combat.driver.api.DriverDefinition definition) {
            check(); return io.github.fatalitaya.combatnexus.combat.driver.internal.DriverRegistrations.register(definition);
        }
        public void itemBindings(NexusId contribution,NexusId item,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec bindings){check();ITEM_BINDINGS.add(contribution,item,bindings);}
        public io.github.fatalitaya.combatnexus.combat.api.definition.CombatDefinitionRegistrar definitions() { check(); return DEFINITIONS; }
        public io.github.fatalitaya.combatnexus.combat.api.platform.EquipmentDamageAccess equipmentDamage() {
            check();
            return io.github.fatalitaya.combatnexus.combat.damage.core.settlement.DamageSettlementTracker::applyEquipment;
        }
        public io.github.fatalitaya.combatnexus.combat.api.platform.NativeDamageAccess nativeDamage() {
            check();
            return io.github.fatalitaya.combatnexus.combat.damage.core.settlement.NativeDamageAdapter.INSTANCE;
        }
        public io.github.fatalitaya.combatnexus.combat.api.platform.EffectOriginAccess effectOrigins() {
            check(); return io.github.fatalitaya.combatnexus.effect.internal.NativeEffectOrigins.INSTANCE;
        }
        public io.github.fatalitaya.combatnexus.combat.api.platform.WorldLifecycleAccess worldLifecycle() {
            check(); return io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.INSTANCE;
        }
        public io.github.fatalitaya.combatnexus.combat.api.platform.NativeEffectsAccess nativeEffects() {
            check(); return io.github.fatalitaya.combatnexus.effect.internal.PlatformEffects.INSTANCE;
        }
        boolean open = true;
        final Map<NexusId,io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy> settlementPolicies=new LinkedHashMap<>();
        public void settlementPolicy(io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy policy){check();if(settlementPolicies.putIfAbsent(policy.id(),policy)!=null)throw new IllegalArgumentException("Duplicate settlement policy: "+policy.id());}
        public <T extends io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent> void intent(io.github.fatalitaya.combatnexus.combat.api.scheduling.IntentType<T> type) { check(); IntentTypes.register(type); }
        final Map<NexusId,io.github.fatalitaya.combatnexus.combat.state.api.StateKey<?>> stateTypes=new LinkedHashMap<>();
        public <T> void runtimeState(io.github.fatalitaya.combatnexus.combat.state.api.StateKey<T> key) { check();if(stateTypes.putIfAbsent(key.id(),key)!=null)throw new IllegalArgumentException("Duplicate runtime state: "+key.id()); }
        final Map<NexusId,Consumer<DefinitionView>> definitionObservers=new LinkedHashMap<>();
        public void observeDefinitions(NexusId id,Consumer<DefinitionView> observer) { check();Objects.requireNonNull(id);Objects.requireNonNull(observer);if(definitionObservers.putIfAbsent(id,observer)!=null)throw new IllegalArgumentException("Duplicate definition observer"); }
        final Map<NexusId,MomentObserver<?>> moments=new LinkedHashMap<>();
        public <T extends io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment> void observeMoment(NexusId id,Class<T> type,Consumer<T> observer) {
            check();Objects.requireNonNull(type);Objects.requireNonNull(observer);
            if(moments.putIfAbsent(id,new MomentObserver<>(Objects.requireNonNull(id),type,observer))!=null)throw new IllegalArgumentException("Duplicate moment observer: "+id);
        }
        final Map<NexusId,java.util.function.Function<io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame,List<io.github.fatalitaya.combatnexus.combat.api.source.SourceDescription>>> descriptions=new LinkedHashMap<>();
        public void sourceDescriptions(NexusId id,java.util.function.Function<io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame,List<io.github.fatalitaya.combatnexus.combat.api.source.SourceDescription>> provider) {
            check();Objects.requireNonNull(id);Objects.requireNonNull(provider);if(descriptions.putIfAbsent(id,provider)!=null)throw new IllegalArgumentException("Duplicate source descriptor: "+id);
        }
        final Map<NexusId, io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy> damageFormulas = new LinkedHashMap<>();
        public void damageFormula(NexusId id, io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy policy) {
            check(); Objects.requireNonNull(id); Objects.requireNonNull(policy);
            if (damageFormulas.putIfAbsent(id, policy) != null) throw new IllegalArgumentException("Duplicate damage formula: " + id);
        }
        final Map<NexusId, Consumer<io.github.fatalitaya.combatnexus.combat.api.source.RestoredSource>> restorationOwners = new LinkedHashMap<>();
        public void restoredSource(NexusId provider, Consumer<io.github.fatalitaya.combatnexus.combat.api.source.RestoredSource> owner) {
            check(); Objects.requireNonNull(provider); Objects.requireNonNull(owner);
            if (restorationOwners.putIfAbsent(provider, owner) != null) throw new IllegalArgumentException("Duplicate restoration owner: " + provider);
        }
        final Map<NexusId, Collector<?>> collectors = new LinkedHashMap<>();
        public <T> void facts(io.github.fatalitaya.combatnexus.combat.api.input.FactKey<T> key,
                java.util.function.Function<net.minecraft.world.entity.LivingEntity, T> collector) {
            check(); Objects.requireNonNull(collector);
            if (collectors.putIfAbsent(key.id(), new Collector<>(key, collector)) != null)
                throw new IllegalArgumentException("Duplicate fact collector: " + key.id());
        }
        final BridgeRegistry bridges = new BridgeRegistry();
        public io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeRegistrar bridges() {
            check(); return capability -> { check(); bridges.register(capability); };
        }
        final CombatListenerRegistry<io.github.fatalitaya.combatnexus.combat.api.source.SourceProvider> providers = new CombatListenerRegistry<>();
        public void sourceProvider(NexusId id, int priority, io.github.fatalitaya.combatnexus.combat.api.source.SourceProvider provider) { check(); providers.register(id, priority, provider); }
        final DomainPipeline.Builder<HealingPhase, Consumer<HealingCalculation>> pipeline = new DomainPipeline.Builder<>(HealingPhase.class);
        final CombatListenerRegistry<Consumer<HealingSettlement>> healObservers = new CombatListenerRegistry<>();
        final CombatListenerRegistry<Consumer<ShieldSettlement>> shieldObservers = new CombatListenerRegistry<>();
        final CombatListenerRegistry<Consumer<CommandResult<?>>> stateObservers = new CombatListenerRegistry<>();
        final CombatListenerRegistry<Consumer<AbilityFact>> abilityObservers=new CombatListenerRegistry<>();
        final Map<NexusId,AbilityReaction> abilityReactions=new HashMap<>();
        final Map<NexusId, Reaction> reactions = new HashMap<>();
        void check() { if (!open) throw new IllegalStateException("Registration callback expired"); }
        public <T extends io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent> void behavior(io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorType<T> type) { check(); BehaviorTypes.register(type); }
        public void effectCapability(NexusId id, io.github.fatalitaya.combatnexus.effect.api.EffectCapability capability) { check(); io.github.fatalitaya.combatnexus.effect.internal.EffectCapabilities.register(id, capability); }
        public void effectStacking(NexusId id, io.github.fatalitaya.combatnexus.effect.api.EffectStackingStrategy strategy) { check(); io.github.fatalitaya.combatnexus.effect.internal.EffectStackingStrategies.register(id, strategy); }
        public void environmentEffect(io.github.fatalitaya.combatnexus.effect.api.EnvironmentEffectDefinition definition) { check(); io.github.fatalitaya.combatnexus.effect.internal.EnvironmentEffects.register(definition); }
        public <T extends io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent> void effectDescriptor(NexusId id, Class<T> type, io.github.fatalitaya.combatnexus.effect.api.EffectBehaviorDescriptor<T> descriptor) { check(); io.github.fatalitaya.combatnexus.effect.internal.EffectBehaviorDescriptors.register(id, type, descriptor); }
        public io.github.fatalitaya.combatnexus.combat.damage.api.display.phrase.RulePhraseRegistry ruleDescriptions() { check(); return io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageDescriptions.registration(); }
        public void effectDisplay(NexusId definition, io.github.fatalitaya.combatnexus.effect.api.EffectDisplay display) { check(); io.github.fatalitaya.combatnexus.effect.internal.EffectMappings.display(definition, display); }
        public void effectCondition(NexusId id, io.github.fatalitaya.combatnexus.effect.api.EffectCondition condition) { check(); io.github.fatalitaya.combatnexus.effect.internal.EffectMappings.condition(id, condition); }
        public HealingRegistrar healing() { check(); return this; }
        public void processor(NexusId id, HealingPhase phase, int priority, Consumer<HealingCalculation> handler) {
            check(); priority(priority); pipeline.register(id, phase, priority, true, handler);
        }
        public void observe(NexusId id, int priority, Consumer<HealingSettlement> observer) { check(); healObservers.register(id, priority, observer); }
        public void observeShield(NexusId id, int priority, Consumer<ShieldSettlement> observer) { check(); shieldObservers.register(id, priority, observer); }
        public void observeAbility(NexusId id,int priority,Consumer<AbilityFact> observer){check();abilityObservers.register(id,priority,observer);}
        public void reactAbility(NexusId id,int priority,Set<CombatOperation> permissions,Consumer<CombatCallback<AbilityFact>> reaction){
            check();priority(priority);var entry=new AbilityReaction(Objects.requireNonNull(id),priority,Set.copyOf(permissions),Objects.requireNonNull(reaction));
            if(abilityReactions.putIfAbsent(id,entry)!=null)throw new IllegalArgumentException("Duplicate ability reaction: "+id);
        }
        public void observeState(NexusId id, int priority, Consumer<CommandResult<?>> observer) { check(); stateObservers.register(id, priority, observer); }
        public void react(NexusId id, int priority, Set<CombatOperation> permissions, Consumer<CombatCallback<HealingSettlement>> handler) {
            check(); priority(priority);
            var entry = new Reaction(Objects.requireNonNull(id), priority, Set.copyOf(permissions), Objects.requireNonNull(handler));
            if (reactions.putIfAbsent(id, entry) != null) throw new IllegalArgumentException("Duplicate reaction: " + id);
        }
        void priority(int priority) { if (priority < -10000 || priority > 10000) throw new IllegalArgumentException("Priority out of range"); }
    }
    public static void calculate(HealingPhase phase, HealingCalculation calculation) {
        healing.execute(phase, entry -> {
            try { calculation.invoke(entry.handler()); }
            catch (Throwable failure) { CombatFailures.rethrowFatal(failure); }
        }, () -> false);
    }
    public static void registerDamageContributions(io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageModuleRegistrar registrar) {
        for (var phase : io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase.values())
            registrar.registerPhaseProcessor(NexusId.fromNamespaceAndPath("combatnexus", "behavior/" + phase.name().toLowerCase(Locale.ROOT)), new io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor() {
                public io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase phase() { return phase; }
                public void apply(io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext ctx) {
                    var participants = new LinkedHashSet<UUID>(); participants.add(ctx.victim().getUUID());
                    if (ctx.logicalAttacker() != null) participants.add(ctx.logicalAttacker().getUUID());
                    for (var participant : participants) for (var source : ctx.input(participant).behaviors())
                        for (var component : source.plan().components()) if (component instanceof io.github.fatalitaya.combatnexus.combat.api.behavior.NumericalBehavior contribution)
                            contribution.damage(phase, ctx, source);
                }
            });
    }

    private static <T> void observe(ServerSession server, T result, List<CombatListenerRegistry.Entry<Consumer<T>>> entries) {
        for (var entry : entries) try (var scope = CombatDispatchScope.observation(OWNER, result, server)) { entry.listener().accept(result); }
        catch (Throwable failure) { CombatFailures.rethrowFatal(failure); }
    }
    public static void healingCompleted(ServerSession server, CombatLineage lineage, HealingSettlement result) {
        for (var entry : healingReactions) {
            var callback = new CombatCallback<>(lineage, result, server);
            var permissions = result.actualRecovery() > 0 ? entry.permissions() : Set.<CombatOperation>of();
            try (var scope = CombatDispatchScope.callback(OWNER, callback, null, server, lineage, permissions)) { entry.handler().accept(callback); }
            catch (Throwable failure) { CombatFailures.rethrowFatal(failure); }
        }
        observe(server, result, healingObservers);
    }
    public static void shieldCompleted(ServerSession server, ShieldSettlement result) { observe(server, result, shieldObservers); }
    public static void abilityCompleted(ServerSession server,CombatLineage lineage,AbilityFact fact){
        if(lineage!=null)for(var entry:abilityReactions){
            var callback=new CombatCallback<>(lineage,fact,server);
            try(var scope=CombatDispatchScope.callback(OWNER,callback,null,server,lineage,entry.permissions())){entry.handler().accept(callback);}
            catch(Throwable failure){CombatFailures.rethrowFatal(failure);}
        }
        observe(server,fact,abilityObservers);
    }
    public static void stateCompleted(ServerSession server, CombatLineage lineage, CommandResult<?> result) { observe(server, result, stateObservers); }
}
