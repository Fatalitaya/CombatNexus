package io.github.fatalitaya.combatnexus.combat.internal;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ServerSession;
import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import io.github.fatalitaya.combatnexus.effect.spi.EffectContributionPort;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;

import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshotService;
import io.github.fatalitaya.combatnexus.combat.healing.spi.HealingApplicationPort;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingService;
import io.github.fatalitaya.combatnexus.combat.regeneration.spi.RegenerationConditions;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationScheduler;
import io.github.fatalitaya.combatnexus.combat.shield.spi.ShieldPoolService;
import io.github.fatalitaya.combatnexus.effect.api.EffectInstance;
import io.github.fatalitaya.combatnexus.effect.api.EffectService;
import io.github.fatalitaya.combatnexus.presentation.api.CombatPanelSnapshot;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Explicit world-session composition root. Construct on the server thread, tick from one adapter,
 * and discard on world unload. The active target owns creation and lifecycle events.
 * The effect executor applies/revokes managed contributions synchronously before values are captured.
 */
public final class CombatRuntime {
    private final io.github.fatalitaya.combatnexus.combat.ability.internal.DefaultAbilityService abilities=new io.github.fatalitaya.combatnexus.combat.ability.internal.DefaultAbilityService(this);
    public io.github.fatalitaya.combatnexus.combat.ability.internal.DefaultAbilityService abilityControl(){requireOwner();return abilities;}
    public io.github.fatalitaya.combatnexus.combat.ability.api.AbilityService abilities(){requireOwner();return abilities.businessView();}
    public java.util.List<io.github.fatalitaya.combatnexus.combat.api.item.EquipmentBindingFact> equipmentFacts(UUID actor){requireOwner();return equipment.facts(actor);}
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.DecisionSamples decisionSamples=new io.github.fatalitaya.combatnexus.combat.internal.coordination.DecisionSamples();
    public io.github.fatalitaya.combatnexus.combat.internal.coordination.DecisionSamples decisionSamples(){requireOwner();return decisionSamples;}
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions bridgeExecutions=new io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions(this);
    public io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCalls bridges() { requireOwner(); return bridgeExecutions; }
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations sources = new io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations(this);
    public io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations sourceInstallations() { requireOwner(); return sources; }
    private final io.github.fatalitaya.combatnexus.combat.driver.internal.WorldDrivers drivers = new io.github.fatalitaya.combatnexus.combat.driver.internal.WorldDrivers(this);
    public io.github.fatalitaya.combatnexus.combat.driver.internal.WorldDrivers driverControl() { requireOwner(); return drivers; }
    private final io.github.fatalitaya.combatnexus.presentation.platform.CombatPresentationNetwork presentationNetwork = new io.github.fatalitaya.combatnexus.presentation.platform.CombatPresentationNetwork();
    public io.github.fatalitaya.combatnexus.presentation.platform.CombatPresentationNetwork presentationNetwork() { requireOwner(); return presentationNetwork; }
    private final Thread owner = Thread.currentThread();
    private final ServerLevel level;
    private final io.github.fatalitaya.combatnexus.combat.api.platform.ParticipantStateAccess participantStates;
    public io.github.fatalitaya.combatnexus.combat.api.platform.ParticipantStateAccess participantStates() { requireOwner(); return participantStates; }
    public void requireActive() { requireOwner(); }
    private final io.github.fatalitaya.combatnexus.combat.time.internal.WorldBusinessTime businessTime;
    public io.github.fatalitaya.combatnexus.combat.time.internal.WorldBusinessTime timeControl() { requireOwner(); return businessTime; }
    private long businessPulse;
    public long businessTick(UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId source) {
        requireOwner(); return businessTime.now(target, source);
    }
    private final java.util.function.LongSupplier gameClock;
    public long gameTick() { requireOwner(); return gameClock.getAsLong(); }
    private final io.github.fatalitaya.combatnexus.combat.api.platform.DamageTypeCatalog damageTypes;
    public io.github.fatalitaya.combatnexus.combat.api.platform.DamageTypeCatalog damageTypes() { requireOwner(); return damageTypes; }
    public java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity> participantIdentity(UUID target) {
        requireOwner();
        var entity = level.getEntity(target);
        if (entity == null || entity.isRemoved()) return java.util.Optional.empty();
        return java.util.Optional.of(new io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity(
                entity.getUUID(), ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance) entity).combatnexus$incarnation()));
    }
    public CombatServerRuntime serverRuntime() { return serverRuntime; }
    public boolean targetAlive(UUID target) { requireOwner(); return level.getEntity(target) instanceof LivingEntity entity && entity.isAlive(); }

    private final io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileDamageWorldAccess projectileDamage;
    public io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileDamageWorldAccess projectileDamage() { requireOwner(); return projectileDamage; }
    private final io.github.fatalitaya.combatnexus.combat.api.platform.HealthWorldAccess health;
    public io.github.fatalitaya.combatnexus.combat.api.platform.HealthWorldAccess healthAccess() { requireOwner(); return health; }
    private final io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionWorldAccess absorption;
    public io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionWorldAccess absorptionAccess() { requireOwner(); return absorption; }
    private final io.github.fatalitaya.combatnexus.combat.api.platform.EntityMotionAccess motion;
    public io.github.fatalitaya.combatnexus.combat.api.platform.EntityMotionAccess motion() { requireOwner(); return motion; }
    private final UUID sessionId;
    private final io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession worldSession;
    private final io.github.fatalitaya.combatnexus.combat.healing.internal.HealingServiceControl healing;
    private final HealingService healingView;
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.CombatReactions reactions;
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.WorldBehaviorSources behaviors;
    private final io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSources behaviorView;
    private final io.github.fatalitaya.combatnexus.combat.api.reaction.CombatReactionService reactionView;
    private final CombatServerRuntime serverRuntime;
    public ServerSession serverSession() { return serverRuntime.session(); }
    public io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession worldSession() { return worldSession; }
    private final io.github.fatalitaya.combatnexus.combat.shield.internal.ShieldPoolServiceControl shields;
    private final ShieldPoolService shieldsView;
    private final io.github.fatalitaya.combatnexus.combat.regeneration.internal.RegenerationSchedulerControl regeneration;
    private final RegenerationScheduler regenerationView;
    private final io.github.fatalitaya.combatnexus.effect.internal.EffectServiceControl effects;
    private final EffectService effectsView;
    private final AttributeSnapshotService attributes = new AttributeSnapshotService();
    private final java.util.Map<UUID, java.util.Set<io.github.fatalitaya.combatnexus.api.id.NexusId>> persistentSources = new java.util.HashMap<>();
    public enum SourcePersistence { SESSION, PERSISTENT }
    public void sourcePersistence(UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId source, SourcePersistence policy) {
        requireOwner(); io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        Objects.requireNonNull(target); Objects.requireNonNull(source); Objects.requireNonNull(policy);
        if (!(level.getEntity(target) instanceof LivingEntity entity) || !entity.isAlive())
            throw new IllegalArgumentException("Persistence policy requires a live target in this world");
        var sources = persistentSources.computeIfAbsent(target, ignored -> new java.util.HashSet<>());
        if (policy == SourcePersistence.PERSISTENT) {
            if (sources.size() >= 64 && !sources.contains(source)) throw new IllegalStateException("Persistent source capacity exceeded");
            sources.add(source);
        } else sources.remove(source);
        if (sources.isEmpty()) persistentSources.remove(target);
    }
    private io.github.fatalitaya.combatnexus.combat.internal.persistence.CombatPersistence persistence() {
        return new io.github.fatalitaya.combatnexus.combat.internal.persistence.CombatPersistence(
                effects, shields, regeneration, behaviors, equipment, sources, persistentSources, gameClock, businessTime, drivers, abilities);
    }
    public String saveLifecycle(UUID target, io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        return persistence().save(target);
    }
    public void restoreLifecycle(UUID target, String encoded, io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        persistence().restore(target, encoded);
    }
    private long tick = -1;
    private boolean closed;
    private final WorldServices services;
    public WorldServices services() { requireOwner(); return services; }
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.EquipmentBehaviorController equipment;

    public CombatRuntime(CombatServerRuntime serverRuntime, io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession worldSession, java.util.function.LongSupplier gameClock, ServerLevel level, HealingApplicationPort healingPort, RegenerationConditions conditions,
                         Predicate<EffectInstance> immunity, io.github.fatalitaya.combatnexus.effect.spi.EffectContributionPort effectExecutor,
                         io.github.fatalitaya.combatnexus.combat.api.platform.EquipmentObservation equipmentObservation, io.github.fatalitaya.combatnexus.combat.api.platform.EntityMotionAccess motion, io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionWorldAccess absorption, io.github.fatalitaya.combatnexus.combat.api.platform.HealthWorldAccess health, io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileDamageWorldAccess projectileDamage, io.github.fatalitaya.combatnexus.combat.api.platform.DamageTypeCatalog damageTypes, io.github.fatalitaya.combatnexus.combat.api.platform.ParticipantStateAccess participantStates) {
        this.level = Objects.requireNonNull(level, "level");
        this.damageTypes = Objects.requireNonNull(damageTypes);
        this.participantStates = Objects.requireNonNull(participantStates);
        this.worldSession = Objects.requireNonNull(worldSession, "World session not bound");
        this.gameClock = Objects.requireNonNull(gameClock);
        this.businessTime = new io.github.fatalitaya.combatnexus.combat.time.internal.WorldBusinessTime(this);
        this.sessionId = worldSession.id();
        this.motion = Objects.requireNonNull(motion, "motion");
        this.absorption = Objects.requireNonNull(absorption, "absorption");
        this.projectileDamage = Objects.requireNonNull(projectileDamage, "projectileDamage");
        this.health = Objects.requireNonNull(health, "health");
        this.serverRuntime = Objects.requireNonNull(serverRuntime);
        serverRuntime.requireThread();
        reactions = new io.github.fatalitaya.combatnexus.combat.internal.coordination.CombatReactions(this);
        equipment = new io.github.fatalitaya.combatnexus.combat.internal.coordination.EquipmentBehaviorController(this, equipmentObservation);
        reactionView = reactions.readOnly();
        var shieldsImplementation = new io.github.fatalitaya.combatnexus.combat.shield.internal.DefaultShieldService(serverRuntime, this, businessTime);
        shields = shieldsImplementation;
        shieldsView = shieldsImplementation.businessView();
        var healingImplementation = new io.github.fatalitaya.combatnexus.combat.healing.internal.DefaultHealingService(serverRuntime, this, healingPort);
        healing = healingImplementation;
        healingView = healingImplementation.businessView();
        var regenerationImplementation = new io.github.fatalitaya.combatnexus.combat.regeneration.internal.DefaultRegenerationScheduler(conditions, businessTime);
        regeneration = regenerationImplementation;
        regenerationView = regenerationImplementation.businessView();
        behaviors = new io.github.fatalitaya.combatnexus.combat.internal.coordination.WorldBehaviorSources(this, reactions, businessTime);
        behaviorView = behaviors.readOnly();
        var effectsImplementation = new io.github.fatalitaya.combatnexus.effect.internal.DefaultEffectService(immunity, behaviors.compose(effectExecutor), businessTime);
        effects = effectsImplementation;
        effectsView = effectsImplementation.businessView();
        services = new WorldServices(this);
        serverRuntime.attach(worldSession(), this);
    }

    /** Expire state before processing actions; adapters apply effect changes before tickRecovery. */
    private void beginTick() {
        requireOwner();
        long now = gameClock.getAsLong();
        if (now < tick) throw new IllegalStateException("World clock moved backwards; recreate the runtime");
        if (now == tick) return;
        serverRuntime.lifecycle(this, () -> { ++businessPulse; effects.tick(businessPulse); shields.tick(businessPulse); effects.drainChanges(); });
        tick = now;
    }

    /** Resource adoption reconciles tracked equipment before subsequent requests capture facts. */
    public void definitionsAdopted(){requireOwner();equipment.tick(serverRuntime.definitions().orElseThrow().generation());sources.refreshAbilities();}
    /** Call after managed contributions and conditions have been reconciled for this tick. */
    public void advanceLifecycle(io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        java.util.Objects.requireNonNull(permit).require(this); beginTick();
    }
    public void tickRecoveryLifecycle(io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        java.util.Objects.requireNonNull(permit).require(this);
        requireTick();
        equipment.tick(serverRuntime.definitions().orElseThrow().generation());
        sources.tick();
        regeneration.tick(businessPulse, healing::submit);
        behaviors.tick(businessPulse);
    }

    /** Called only by the authenticated external time driver, outside any combat transaction. */
    public void advanceBusinessTime() {
        requireOwner();
        serverRuntime.lifecycle(this, () -> {
            ++businessPulse;
            effects.tick(businessPulse); shields.tick(businessPulse); effects.drainChanges();
            regeneration.tick(businessPulse, healing::submit); behaviors.tick(businessPulse);
            if (level.getEntity(businessTime.processingTarget()) instanceof LivingEntity living)
                io.github.fatalitaya.combatnexus.combat.internal.coordination.ScheduledOperations.tick(living);
        });
    }

    private final java.util.List<Holder<Attribute>> attributeCatalog = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.holders().<Holder<Attribute>>map(holder -> holder).toList();
    private final java.util.List<io.github.fatalitaya.combatnexus.api.id.NexusId> allAttributeIds = attributeIds(attributeCatalog);
    public java.util.List<Holder<Attribute>> attributeCatalog() { requireOwner(); return attributeCatalog; }
    private static java.util.List<io.github.fatalitaya.combatnexus.api.id.NexusId> attributeIds(Iterable<Holder<Attribute>> requested) {
        var ids = new java.util.ArrayList<io.github.fatalitaya.combatnexus.api.id.NexusId>();
        for (var holder : requested) ids.add(io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().keyId(holder.unwrapKey().orElseThrow()));
        return ids;
    }
    public AttributeSnapshot captureAttributes(LivingEntity entity, Iterable<Holder<Attribute>> requested) {
        requireEntity(entity);
        return attributes.capture(entity.getUUID(), io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().attributeObservation(entity), requested == attributeCatalog ? allAttributeIds : attributeIds(requested));
    }

    public AttributeSnapshot captureAttributes(LivingEntity entity, Iterable<Holder<Attribute>> requested,
                                                java.util.function.IntConsumer budget) {
        requireEntity(entity);
        return attributes.capture(entity.getUUID(), io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().attributeObservation(entity), requested == attributeCatalog ? allAttributeIds : attributeIds(requested), budget);
    }

    public java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame> participantInput(UUID target, long generation) {
        requireOwner();
        var entity = level.getEntity(target);
        return entity instanceof LivingEntity living && !living.isRemoved()
                ? java.util.Optional.of(captureInput(living, generation)) : java.util.Optional.empty();
    }
    public java.util.Optional<io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture.Participant> participantCapture(UUID target, long generation) {
        requireOwner();
        return level.getEntity(target) instanceof LivingEntity living && !living.isRemoved()
            ? java.util.Optional.of(captureNativeInput(living, generation)) : java.util.Optional.empty();
    }
    public io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame captureInput(LivingEntity entity, long generation) {
        return captureNativeInput(entity, generation).frame();
    }
    private io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture.Participant captureNativeInput(LivingEntity entity, long generation) {
        requireEntity(entity);
        var snapshot = captureAttributes(entity, attributeCatalog);
        var captured = io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture.capture(entity);
        var frame = new io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame(sessionId,
                ContentAccess.get().keyId(level.dimension()), entity.getUUID(), snapshot.revision(), snapshot.gameTick(),
                generation, snapshot.valueFrame(), snapshot.provenance(), captured.facts(), behaviors.capture(entity.getUUID()));
        return new io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture.Participant(frame, captured);
    }

    /** The same snapshot object can be passed to combat consumers and to this presentation projection. */
    public CombatPanelSnapshot panel(LivingEntity entity, AttributeSnapshot snapshot) {
        return panel(entity, snapshot, count -> {});
    }
    public CombatPanelSnapshot panel(LivingEntity entity, AttributeSnapshot snapshot, java.util.function.IntConsumer budget) {
        requireEntity(entity);
        if (attributes.latest(entity.getUUID()).orElse(null) != snapshot || snapshot.gameTick() != tick) {
            throw new IllegalArgumentException("Expected this runtime's current authoritative attribute snapshot");
        }
        var behaviorViews = behaviors.view(entity.getUUID(), budget);
        var reactionFacts = reactions.facts(entity.getUUID(), budget);
        var input=new io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame(sessionId,ContentAccess.get().keyId(level.dimension()),entity.getUUID(),snapshot.revision(),snapshot.gameTick(),
                serverRuntime.definitions().orElseThrow().generation(),snapshot.valueFrame(),snapshot.provenance(),io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture.captureFacts(entity, budget),behaviors.capture(entity.getUUID()));
        var descriptions=io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.describe(input);budget.accept(descriptions.size());
        return new CombatPanelSnapshot(snapshot, entity.getHealth(), entity.getAbsorptionAmount(),
                shields.snapshot(entity.getUUID(), budget), input.facts().effectViews(), behaviorViews.stream().map(view -> new CombatPanelSnapshot.BehaviorObservation(view.source().target(), view.source().effect(), view.source().source(), view.source().installation(), view.protectionAvailable(), view.source().plan().components().stream().map(io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent::type).toList())).toList(), reactionFacts, descriptions, sources.snapshot(entity.getUUID()), equipment.projection(entity.getUUID()).stream().map(e->new CombatPanelSnapshot.EquipmentView(e.installation(),e.slot(),e.binding(),e.definition(),e.selected())).toList());
    }

    /** Death, respawn, world exit and dimension transfer reset all local counters and owned state. */
    public void clearLifecycle(UUID entity, EffectService.Removal reason, io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        java.util.Objects.requireNonNull(permit).require(this);
        requireOwner();
        serverRuntime.lifecycle(this, () -> {
        persistentSources.remove(entity);
        drivers.clear(entity);

        effects.clear(entity, reason);
        equipment.clear(entity);
        sources.clear(entity);
        abilities.clear(entity);
        regeneration.clear(entity);
        behaviors.clear(entity);
        shields.clear(entity);
        attributes.clear(entity);
        presentationNetwork.invalidateEntity(entity);
        businessTime.clear(entity);
        });
    }

    public boolean commandSessionClosed() { return closed; }
    public ServerLevel level() { requireOwner(); return level; }
    public io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSources behaviors() { requireOwner(); return behaviorView; }
    public io.github.fatalitaya.combatnexus.combat.api.reaction.CombatReactionService reactions() { requireOwner(); return reactionView; }
    public void consumeAbilityPeriodic(io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource source,io.github.fatalitaya.combatnexus.api.id.NexusId contribution){requireOwner();behaviors.consumeExternalPeriodic(source,contribution);}
    public java.util.Optional<String> claimAbilityPeriodic(io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource source,io.github.fatalitaya.combatnexus.api.id.NexusId contribution,io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit limit){requireOwner();return reactions.claimAbilityPeriodic(source,contribution,limit);}
    public java.util.Optional<String> claimAbilityTrigger(io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource source,io.github.fatalitaya.combatnexus.api.id.NexusId contribution,io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit limit,io.github.fatalitaya.combatnexus.combat.api.action.CombatAction action){requireOwner();return reactions.claimAbilityTrigger(source,contribution,limit,action);}
    public void dispatchSettlement(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatCallback<io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementEnvelope> callback,
            java.util.List<io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource> captured,
            io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this);requireOwner();reactions.completed(callback,captured);
    }
    public void dispatchDamageReaction(io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementCallback callback,
            io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); reactions.damage(callback);
    }
    public void recordRecoveryDamage(io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementCallback callback,
            io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner();
        if (!CombatDispatchScope.delivering(callback, serverSession()) || callback.context().level() != level)
            throw new IllegalStateException("Recovery delay requires this world's active damage settlement");
        var snapshot = callback.snapshot();
        if (snapshot.successfulHit())
            if (callback.context().target() != null) regeneration.recordDamage(snapshot.target().entity(), businessTick(snapshot.target().entity(), io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("combatnexus", "regeneration")));
    }
    public void dispatchHealingReaction(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatCallback<io.github.fatalitaya.combatnexus.combat.healing.api.HealingSettlement> callback,
            io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); reactions.healing(callback);
    }
    public io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision decideProtection(java.util.UUID target,
            java.util.List<io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource> captured, float before, float proposed, io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs inputs, java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.CombatSource> origin,
            io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); return behaviors.protect(target, captured, before, proposed, inputs, origin);
    }
    public java.util.Map<io.github.fatalitaya.combatnexus.api.id.NexusId, io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorDefinition> behaviorDefinitions() {
        requireOwner(); return definitions().behaviors();
    }
    public HealingService healing() { requireOwner(); return healingView; }
    public void equipmentSourcesChanged(UUID target) { requireOwner(); equipment.invalidate(target); }
    public void equipmentChanged(UUID target, io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition slot, io.github.fatalitaya.combatnexus.combat.api.behavior.EquipmentItem current,
            io.github.fatalitaya.combatnexus.combat.spi.CombatEquipmentAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); equipment.changed(target, slot, current);
    }
    public void trackEquipment(UUID target, io.github.fatalitaya.combatnexus.combat.spi.CombatEquipmentAccess.Permit permit) {
        Objects.requireNonNull(permit).require(this); requireOwner(); equipment.track(target); sources.track(target);
    }
    public DomainDefinitions domainDefinitions() {
        requireOwner(); return definitions().domains();
    }
    /** Future snapshot transport includes this ID so clients reject packets from a prior world session. */
    public UUID sessionId() { requireOwner(); return sessionId; }
    public io.github.fatalitaya.combatnexus.combat.internal.coordination.WorldBehaviorSources behaviorControl(){requireOwner();return behaviors;}
    public io.github.fatalitaya.combatnexus.effect.internal.EffectServiceControl effectControl(){requireOwner();return effects;}
    public io.github.fatalitaya.combatnexus.combat.shield.internal.ShieldPoolServiceControl shieldControl(){requireOwner();return shields;}
    public io.github.fatalitaya.combatnexus.combat.regeneration.internal.RegenerationSchedulerControl regenerationControl(){requireOwner();return regeneration;}
    public io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration definitions() {
        requireOwner();
        var active = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.currentLineage(serverSession());
        if (active.isPresent() && serverRuntime.hasTicket(active.get())) {
            var ticket = serverRuntime.ticket(active.get());
            if (ticket.hasDefinitionBinding()) return ticket.definitions();
        }
        return serverRuntime.definitions().orElseThrow();
    }
    public io.github.fatalitaya.combatnexus.combat.internal.definition.EquipmentSelection equipmentSelection(UUID target){requireOwner();return equipment.selection(target);}
    public ShieldPoolService shields() { requireOwner(); return shieldsView; }
    public java.util.List<io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationPlan> regenerationPlans(UUID target) { requireOwner(); return regeneration.snapshot(target); }
    public RegenerationScheduler regeneration() { requireOwner(); return regenerationView; }
    public EffectService effects() { requireOwner(); return effectsView; }

    private void requireEntity(LivingEntity entity) {
        requireTick();
        if (entity.level() != level) throw new IllegalArgumentException("Entity belongs to another world session");
    }
    private void requireTick() {
        requireOwner();
        if (tick < 0 || tick != gameClock.getAsLong()) throw new IllegalStateException("World lifecycle has not advanced to the current tick");
    }
    private void requireOwner() {
        serverRuntime.requireThread();
        if (closed || Thread.currentThread() != owner || !serverRuntime.isOwningThread()) {
            throw new IllegalStateException("Combat runtime requires its owning server thread and an open session");
        }
    }
    /** World unload only. Entity contributions must be revoked by the adapter before closing. */
    public void closeLifecycle(io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess.Permit permit) {
        java.util.Objects.requireNonNull(permit).require(this);
        if (closed) return;
        requireOwner();
        if (io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.isActive()
                || io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope.isActive()) {
            throw new IllegalStateException("Cannot close a world runtime during combat execution or delivery");
        }
        serverRuntime.drain();
        effects.close();
        equipment.close();
        sources.close();
        abilities.close();
        regeneration.close();
        behaviors.close();
        reactions.clear();
        shields.close();
        healing.close();
        attributes.clear();
        presentationNetwork.reset();
        persistentSources.clear();
        drivers.close();
        businessTime.close();
        serverRuntime.detach(worldSession(), this);
        closed = true;
    }
}
