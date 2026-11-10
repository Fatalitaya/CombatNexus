package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;
import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.critical.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageCapture;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile;
import io.github.fatalitaya.combatnexus.combat.damage.core.DamageComponent;
import io.github.fatalitaya.combatnexus.combat.damage.core.config.DamageSettingsStore;
import io.github.fatalitaya.combatnexus.combat.damage.core.contribution.DamageContributionCollector;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.DatapackDamageRuleStore;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.ExternalItemRuleSnapshot;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.ExternalItemRuleSources;
import io.github.fatalitaya.combatnexus.combat.damage.core.template.DamageTemplateSnapshot;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

public final class DamageModuleContext implements DamagePhaseAccess {
    private final java.util.Map<java.util.UUID, io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageParticipantFacts> ruleFacts = new java.util.HashMap<>();
    private final DamageTypeFacts ruleDamageType;
    @Override public io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageParticipantFacts targetFacts() { return ruleFacts.get(victim.getUUID()); }
    @Override public java.util.Optional<io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageParticipantFacts> attackerFacts() { return java.util.Optional.ofNullable(logicalAttacker()).map(entity -> ruleFacts.get(entity.getUUID())); }
    @Override public java.util.Optional<NexusId> damageType() { return ruleDamageType.id(); }
    @Override public java.util.Set<NexusId> damageTypeTags() { return ruleDamageType.tags(); }
    @Override public DamageTriggerPolicy triggerPolicy() { return origin.triggerPolicy(); }
    @Override public boolean hasParentDamage() { return origin.lineage().hasParent(); }

    @Override public float offensiveAmount() {
        if (currentPhase().ordinal() < DamagePhase.MITIGATION_SETUP.ordinal())
            throw new IllegalStateException("Offensive amount has not been frozen");
        return result.offensiveTotal();
    }
    @Override public float defenseInput(io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput input) {
        java.util.Objects.requireNonNull(input);
        float initial = switch (input) {
            case ARMOR -> Math.max(0, getVictimAttrOrZero(io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes.armor()));
            case PROTECTION -> nativeInput.protectionScore();
        };
        return combat.defenseInput(input, initial);
    }
    public float consumeDefenseInput(io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput input) {
        java.util.Objects.requireNonNull(input);
        if (planFrozen || !io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.isCurrentPhase(
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.COMPUTE)
                || !((ServerLevel) victim.level()).getServer().isSameThread() || currentPhase() != DamagePhase.MITIGATION_SETUP)
            throw new IllegalStateException("Defense consumption requires active mitigation setup");
        var owner = input == io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput.ARMOR ? nativeInput.armor() : nativeInput.enchantments();
        if (owner != io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput.Reduction.COMBATNEXUS)
            throw new IllegalStateException("Defense input is not owned by CombatNexus");
        combat.consume(input);
        return defenseInput(input);
    }
    @Override public DamageMutationResult tryPenetrateDefense(io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput input,
            float ratio, float flat, NexusId source) {
        if (planFrozen || !io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.isCurrentPhase(
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.COMPUTE)
                || !((ServerLevel) victim.level()).getServer().isSameThread())
            throw new IllegalStateException("Defense inputs require an active calculation");
        if (currentPhase() != DamagePhase.MITIGATION_SETUP) return DamageMutationResult.REJECTED_WRONG_PHASE;
        if (input == null || source == null || ratio < 0 || flat < 0) return DamageMutationResult.REJECTED_INVALID_DEFENSE_INPUT;
        if (!Float.isFinite(ratio) || !Float.isFinite(flat)) return DamageMutationResult.REJECTED_NON_FINITE;
        if (phaseState.defensiveLocked() || combat.consumed(input)) return DamageMutationResult.REJECTED_DEFENSE_LOCKED;
        var owner = input == io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput.ARMOR ? nativeInput.armor() : nativeInput.enchantments();
        if (owner != io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput.Reduction.COMBATNEXUS)
            return DamageMutationResult.REJECTED_DEFENSE_NOT_OWNED;
        if (ratio == 0 && flat == 0) return DamageMutationResult.NO_OP_ZERO;
        float before = defenseInput(input);
        float after = Math.max(0, before * (1 - Math.min(1, ratio)) - flat);
        combat.setDefenseInput(input, after);
        recordContribution(DamageMutationResult.APPLIED, new io.github.fatalitaya.combatnexus.combat.damage.api.display.DefensePenetrationFact(
                source, input, ratio, flat, before, after, io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageContributionStatus.APPLIED));
        return DamageMutationResult.APPLIED;
    }
    private final io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput nativeInput;
    @Override public io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput nativeInput() { return nativeInput; }
    @Override public java.util.List<DamageChannel> activeChannels() {
        var channels = new java.util.ArrayList<DamageChannel>();
        for (int i = 0; i < getActiveComponentCount(); i++) channels.add(getActiveComponent(i).channel);
        return java.util.List.copyOf(channels);
    }
    @Override public void recordContribution(DamageMutationResult result,
            io.github.fatalitaya.combatnexus.combat.damage.api.display.ContributionRecord fact) {
        if (planFrozen || !io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.isCurrentPhase(
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.COMPUTE)
                || !((ServerLevel) victim.level()).getServer().isSameThread())
            throw new IllegalStateException("Contributions require an active calculation");
        contributions.recordFact(java.util.Objects.requireNonNull(result), java.util.Objects.requireNonNull(fact));
    }
    private final java.util.List<io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Step> beforeSteps = new java.util.ArrayList<>();
    private boolean planFrozen;
    @Override public void beforeApplication(io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Step step) {
        if (planFrozen || !io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.isCurrentPhase(
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.COMPUTE))
            throw new IllegalStateException("Plan is only mutable during its calculation");
        beforeSteps.add(java.util.Objects.requireNonNull(step));
    }

    private final java.util.Map<java.util.UUID, io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot>
            attributeSnapshots;

    private final java.util.Map<java.util.UUID, io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame> inputFrames = new java.util.LinkedHashMap<>();
    public io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame input(java.util.UUID entity) {
        return java.util.Objects.requireNonNull(inputFrames.get(entity), "Participant not captured");
    }
    private final java.util.Map<java.util.UUID, io.github.fatalitaya.combatnexus.combat.api.input.CapturedItem> nativeWeapons = new java.util.HashMap<>();
    io.github.fatalitaya.combatnexus.combat.api.input.CapturedItem nativeWeapon(java.util.UUID participant) { return java.util.Objects.requireNonNull(nativeWeapons.get(participant)); }
    private final java.util.Map<java.util.UUID, io.github.fatalitaya.combatnexus.combat.api.input.CombatEntityFacts> participantFacts = new java.util.HashMap<>();
    @Override public io.github.fatalitaya.combatnexus.combat.api.input.CombatEntityFacts facts(java.util.UUID entity) {
        return java.util.Objects.requireNonNull(participantFacts.get(entity), "Entity was not captured for this request");
    }
    private final io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration definitions;
    public io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration definitions() { return definitions; }
    private final DamageSettingsStore.Snapshot settings;
    public DamageChannel resolveChannel(NexusId id) { return definitions.damage().channels().channel(id); }
    public DamageSettingsStore.Snapshot settings() { return settings; }

    private final @Nullable LivingEntity attacker;
    private final LivingEntity victim;
    private final DamageSource source;
    private final DamageOrigin origin;
    private final boolean isManaged;
    private final boolean isVanillaJumpCrit;

    private final DamageEventSnapshot event;
    private final DamageResultWriter eventWriter;
    private final DamageSourceContext sourceContext;
    private final DamageContextMutations mutations;
    private final NativeDamageRuleContext restrictedView;
    private final NativeDamageContextView readOnlyView = new ReadOnlyDamageContext(this);
    NativeDamageContextView readOnlyView() { return readOnlyView; }
    void processorFailed(NexusId processor, DamagePhase phase, String callback, Exception failure) {
        try {
            CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding)((ServerLevel)victim.level()).getServer())
                .recordProcessorFailure(processor, phase, callback, damageId(), failure);
        } catch (RuntimeException ignored) {
            // Diagnostics cannot change tolerant callback settlement.
        }
    }

    private final DamagePipelineResult result = new DamagePipelineResult();
    private final DamagePacketState packet;
    private final DamageCombatState combat = new DamageCombatState();

    private final float eventOriginalAmount;
    private final float initialBaseAmount;

    private final DamagePhaseState phaseState = new DamagePhaseState();

    private final long damageId;
    private final DamageContributionCollector contributions =
            new DamageContributionCollector();
    private @Nullable List<ExternalItemRuleSnapshot> externalItemRuleSources;
    private final DamageTemplateSnapshot templateSnapshot;
    private final DatapackDamageRuleStore.Snapshot datapackRuleSnapshot;

    DamageModuleContext(DamageModuleContextSpec spec) {
        this.event = DamageEventSnapshot.create(
                spec.event(),
                spec.attacker(),
                spec.victim(),
                spec.initialBaseAmount()
        );

        this.attacker = this.event.attacker();
        this.victim = this.event.victim();
        this.source = this.event.source();
        this.definitions = java.util.Objects.requireNonNull(spec.definitions(), "Request definition handle");
        this.settings = new DamageSettingsStore.Snapshot(definitions.generation(), definitions.settings());
        this.packet = new DamagePacketState(definitions.damage().channels());
        this.origin = Objects.requireNonNull(spec.origin(), "Damage origin");

        this.eventOriginalAmount = this.event.eventOriginalAmount();
        this.initialBaseAmount = this.event.initialBaseAmount();

        this.sourceContext = DamageSourceContext.create(
                this.source,
                this.attacker,
                this.victim,
                this.origin.attribution().directEntity(),
                spec.sourceProfile(),
                spec.vanillaSnapshot(),
                spec.rebuildVanillaOffensiveMobEffects(),
                spec.rebuildVanillaOffensiveEnchantment(),
                spec.rebuildVanillaPreEventDelta(),
                spec.vanillaOffensiveMobEffectDelta(),
                spec.initialBaseBucket(),
                spec.vanillaOffensiveMobEffectBucket(),
                spec.vanillaOffensiveEnchantmentBucket(), definitions.damage().channels()
        );

        this.isManaged = sourceContext.managed();
        this.isVanillaJumpCrit = sourceContext.vanillaJumpCrit();

        this.damageId = origin.lineage().damageId();
        io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (((ServerLevel) victim.level()).getServer())).world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) ((ServerLevel) victim.level())).combatnexus$worldSession())
                .ifPresent(io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess::beginTick);
        for (LivingEntity participant : java.util.Arrays.asList(attacker, logicalAttacker(), victim, equipmentOwner(),
                source.getEntity() instanceof LivingEntity living ? living : null)) {
            if (participant != null) participantFacts.computeIfAbsent(participant.getUUID(), ignored -> {
                var frozen = frozenAttackInput(participant);
                if (frozen.isPresent()) {
                    var weapon = projectileLaunch().filter(l -> l.shooter().entity().equals(participant.getUUID())).map(io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch::weapon)
                        .or(() -> io.github.fatalitaya.combatnexus.combat.spi.CombatActionAccess.weapon(frozen.get().sessionId(), participant.getUUID())).orElseThrow();
                    nativeWeapons.put(participant.getUUID(), weapon);
                    return frozen.get().facts();
                }
                var captured = io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture.capture(participant);
                nativeWeapons.put(participant.getUUID(), captured.weapon());
                return captured.facts();
            });
        }
        projectileLaunch().filter(launch->!launch.shooter().entity().equals(victim.getUUID())).ifPresent(launch->participantFacts.put(launch.shooter().entity(),launch.shooter().facts()));
        this.templateSnapshot = definitions.damage().templates();
        this.datapackRuleSnapshot = definitions.damage().rules();
        this.attributeSnapshots = captureParticipantAttributes(
                java.util.Arrays.asList(attacker, logicalAttacker(), victim, equipmentOwner(),
                        source.getEntity() instanceof LivingEntity living ? living : null));
        projectileLaunch().filter(launch->!launch.shooter().entity().equals(victim.getUUID())).ifPresent(launch->inputFrames.put(launch.shooter().entity(),launch.shooter()));
        for (LivingEntity participant : java.util.Arrays.asList(attacker, logicalAttacker(), victim, equipmentOwner(),
                source.getEntity() instanceof LivingEntity living ? living : null)) {
            if (participant == null || inputFrames.containsKey(participant.getUUID())) continue;
            var frozen = frozenAttackInput(participant);
            if (frozen.isPresent()) { inputFrames.put(participant.getUUID(), frozen.get()); continue; }
            var level = (ServerLevel) participant.level();
            var runtime = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer()))
                    .world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession()).orElseThrow();
            var attributes = attributeSnapshots.get(participant.getUUID());
            inputFrames.put(participant.getUUID(), new io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame(
                    runtime.sessionId(), ContentAccess.get().keyId(level.dimension()), participant.getUUID(), attributes.revision(),
                    attributes.gameTick(), definitions.generation(), attributes.valueFrame(), attributes.provenance(), participantFacts.get(participant.getUUID()), runtime.behaviors().capture(participant.getUUID())));
        }

        this.eventWriter = Objects.requireNonNull(spec.writer(), "writer");

        this.mutations = new DamageContextMutations(
                this.packet,
                this.combat,
                this.result,
                this.phaseState
        );

        participantFacts.forEach((id, facts) -> ruleFacts.put(id, facts.ruleFacts()));
        this.ruleDamageType = ContentAccess.get().damageTypeFacts(source);
        this.nativeInput = NativeDamageInputs.capture(this);
        this.restrictedView = new RestrictedDamageRuleContext(this);
        this.externalItemRuleSources = ExternalItemRuleSources.snapshot(this);
    }

    private java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame> frozenAttackInput(LivingEntity participant) {
        if (participant.getUUID().equals(victim.getUUID())) return java.util.Optional.empty();
        var launch = projectileLaunch().filter(l -> l.shooter().entity().equals(participant.getUUID()));
        if (launch.isPresent()) return java.util.Optional.of(launch.get().shooter());
        var level = (ServerLevel)victim.level();
        var world = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer())).world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession()).orElseThrow();
        return io.github.fatalitaya.combatnexus.combat.spi.CombatActionAccess.input(world.sessionId(), participant.getUUID());
    }

    private java.util.Map<java.util.UUID, io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot> captureParticipantAttributes(
            Iterable<LivingEntity> participants) {
        var snapshots = new java.util.LinkedHashMap<java.util.UUID, io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot>();
        for (LivingEntity participant : participants) {
            if (participant != null && !snapshots.containsKey(participant.getUUID())) {
                var frozen = frozenAttackInput(participant);
                if (frozen.isPresent()) { snapshots.put(participant.getUUID(), frozen.get().attributeSnapshot()); continue; }
                var level = (ServerLevel) participant.level();
                var runtime = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer()))
                        .world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession()).orElseThrow(() -> new IllegalStateException("Participant world session unavailable"));
                snapshots.put(participant.getUUID(), runtime.captureAttributes(participant, runtime.attributeCatalog()));
            }
        }
        projectileLaunch().filter(launch->!launch.shooter().entity().equals(victim.getUUID())).ifPresent(launch->snapshots.put(launch.shooter().entity(),launch.shooter().attributeSnapshot()));
        return java.util.Map.copyOf(snapshots);
    }

    @Override
    public DamagePhase currentPhase() {
        return phaseState.currentPhase();
    }

    public DamageComponent getOrCreateComponent(DamageChannel rawChannel) {
        return packet.getOrCreateComponent(rawChannel);
    }

    /*
     * ---------------------------------------------------------------------
     * Offensive canonical API
     * ---------------------------------------------------------------------
     */

    public int getActiveComponentCount() {
        return packet.activeComponentCount();
    }

    public DamageComponent getActiveComponent(int activeIndex) {
        return packet.activeComponent(activeIndex);
    }

    private DamageComponent findActiveComponent(DamageChannel rawChannel) {
        return packet.findActiveComponent(rawChannel);
    }

    @Override
    public DamageMutationResult tryAddBaseDamage(
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId
    ) {
        return mutations.tryAddBaseDamage(
                channel,
                bucket,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddBaseDamage(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return mutations.tryAddBaseDamage(
                channel,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddChannelPreMultiplier(
            DamageChannel channel,
            int modifierId,
            float value,
            String sourceId
    ) {
        return mutations.tryAddChannelPreMultiplier(
                channel,
                modifierId,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddApplicationPreMultiplier(
            DamageApplicationBucket bucket,
            int modifierId,
            float value,
            String sourceId
    ) {
        return mutations.tryAddApplicationPreMultiplier(
                bucket,
                modifierId,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddGlobalPreMultiplier(
            int modifierId,
            float value,
            String sourceId
    ) {
        return mutations.tryAddGlobalPreMultiplier(
                modifierId,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddChannelPostMultiplier(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return mutations.tryAddChannelPostMultiplier(
                channel,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddGlobalPostMultiplier(
            float value,
            String sourceId
    ) {
        return mutations.tryAddGlobalPostMultiplier(
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryConvertDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio,
            String sourceId
    ) {
        return mutations.tryConvertDamage(
                from,
                to,
                ratio,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryGainExtraDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio,
            String sourceId
    ) {
        return mutations.tryGainExtraDamage(
                from,
                to,
                ratio,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddTrueDamage(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return mutations.tryAddTrueDamage(
                channel,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryMultiplyArmorEffectiveness(
            float multiplier,
            String sourceId
    ) {
        return mutations.tryMultiplyArmorEffectiveness(
                multiplier,
                sourceId
        );
    }

    public float getArmorEffectivenessMultiplier() {
        return combat.armorEffectivenessMultiplier();
    }

    @Override
    public DamageMutationResult tryAddTemporaryResistance(
            DamageChannel channel,
            float rating,
            String sourceId
    ) {
        return mutations.tryAddTemporaryResistance(
                channel,
                rating,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddChannelMitigation(
            DamageChannel channel,
            float reductionPercent,
            String sourceId
    ) {
        return mutations.tryAddChannelMitigation(
                channel,
                reductionPercent,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddGlobalMitigation(
            float reductionPercent,
            String sourceId
    ) {
        return mutations.tryAddGlobalMitigation(
                reductionPercent,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryOverrideFinalDamage(
            float amount,
            String sourceId
    ) {
        return mutations.tryOverrideFinalDamage(
                amount,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryCancelDamage(String sourceId) {
        return mutations.tryCancelDamage(sourceId);
    }

    /*
     * ---------------------------------------------------------------------
     * State / vanilla accessors
     * ---------------------------------------------------------------------
     */

    public DamageMutationResult tryAddVanillaBaseReconstructedDamage(
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId
    ) {
        DamageMutationResult phaseResult =
                mutations.requirePhase(
                        "addVanillaBaseReconstructedDamage",
                        DamagePhase.BASE_MODIFICATION
                );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        return tryAddVanillaReconstructedDamageInternal(
                channel,
                bucket,
                value,
                sourceId,
                "addVanillaBaseReconstructedDamage"
        );
    }

    public DamageMutationResult tryAddVanillaCriticalBonusDamage(
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId
    ) {
        DamageMutationResult phaseResult =
                mutations.requirePhase(
                        "addVanillaCriticalBonusDamage",
                        DamagePhase.CRITICAL_HIT
                );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        return tryAddVanillaReconstructedDamageInternal(
                channel,
                bucket,
                value,
                sourceId,
                "addVanillaCriticalBonusDamage"
        );
    }

    private DamageMutationResult tryAddVanillaReconstructedDamageInternal(
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId,
            String operationName
    ) {
        return mutations.addBaseDamageWithoutPhaseCheck(
                operationName,
                channel,
                bucket,
                value,
                sourceId
        );
    }

    public boolean isDamageCancelled() {
        return result.damageCancelled();
    }

    public float getOffensiveTotal() {
        return result.offensiveTotal();
    }

    public float getFinalEventDamage() {
        return result.finalEventDamage();
    }

    public VanillaDamageCapture.OffensiveSnapshot getVanillaSnapshot() {
        return sourceContext.vanillaSnapshot();
    }

    public float getEventOriginalAmount() {
        return eventOriginalAmount;
    }

    public float getInitialBaseAmount() {
        return initialBaseAmount;
    }

    public VanillaDamageSourceProfile vanillaSourceProfile() {
        return sourceContext.vanillaSourceProfile();
    }

    public void markCritical() {
        combat.markCritical();
    }

    public boolean isCritical() {
        return combat.critical();
    }

    @Override
    public CriticalDecisionSnapshot criticalDecision() {
        return combat.criticalDecision().snapshot();
    }

    List<CriticalDecisionContribution> criticalDecisionContributions() {
        return combat.criticalDecision().orderedContributions();
    }

    @ApiStatus.Internal
    public void addCriticalDecisionContribution(
            CriticalDecisionContribution contribution
    ) {
        combat.criticalDecision().add(contribution);
    }

    CriticalDecision resolveCriticalDecisionContributions() {
        return combat.criticalDecision().resolveContributions();
    }

    boolean claimCriticalProviderCollection() {
        return combat.criticalDecision().claimProviderCollection();
    }

    void freezeCriticalDecision(
            CriticalDecision decision,
            boolean critical,
            CriticalDecisionOutcome outcome,
            boolean chanceSampled
    ) {
        combat.criticalDecision().freeze(
                decision, critical, outcome, chanceSampled);
    }

    boolean criticalEffectApplied() {
        return combat.criticalDecision().effectApplied();
    }

    void markCriticalEffectApplied() {
        combat.criticalDecision().markEffectApplied();
    }

    float vanillaCriticalMultiplier() {
        return sourceContext.vanillaCriticalMultiplier();
    }

    public void setArmorHandled() {
        combat.markArmorHandled();
    }

    public boolean isArmorHandled() {
        return combat.armorHandled();
    }

    public DamagePhase getCurrentProcessingPhase() {
        return phaseState.currentPhase();
    }

    /*
     * ---------------------------------------------------------------------
     * Finalization
     * ---------------------------------------------------------------------
     */

    void setCurrentProcessingPhase(DamagePhase phase) {
        this.phaseState.setCurrentPhase(phase);
    }

    @Override
    public Optional<io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot> attributeSnapshot(
            java.util.UUID entity) {
        return Optional.ofNullable(attributeSnapshots.get(entity));
    }

    private float snapshotValue(LivingEntity entity, NexusId attribute) {
        if (entity == null || attribute == null) return 0.0f;
        var snapshot = attributeSnapshots.get(entity.getUUID());
        return snapshot == null ? 0.0f : snapshot.valueOrZero(attribute);
    }

    public float getAttackerAttrOrZero(NexusId attrHolder) {
        return snapshotValue(attacker, attrHolder);
    }

    public float getLogicalAttackerAttrOrZero(NexusId attrHolder) {
        return snapshotValue(logicalAttacker(), attrHolder);
    }

    public float getVictimAttrOrZero(NexusId attrHolder) {
        return snapshotValue(victim, attrHolder);
    }

    void finalizeOffensiveDamage() {
        if (phaseState.offensiveLocked()) {
            return;
        }

        float finalTotal = 0.0f;

        for (int i = 0; i < packet.activeComponentCount(); i++) {
            DamageComponent component = packet.activeComponent(i);

            component.calculateFinalOffensive(
                    packet.globalPreMultipliers(),
                    packet.globalPostMultipliers()
            );

            float componentFinal = component.getFinalizedOffensiveAmount();
            finalTotal += componentFinal;

        }

        result.setOffensiveTotal(finalTotal);
        phaseState.lockOffense();
    }

    /*
     * ---------------------------------------------------------------------
     * Internal helpers
     * ---------------------------------------------------------------------
     */

    void calculateDefensiveDamage() {
        float finalMitigatedTotal = 0.0f;

        for (int i = 0; i < packet.activeComponentCount(); i++) {
            DamageComponent component = packet.activeComponent(i);

            component.calculateFinalDefensive(packet.globalMitigations());
            finalMitigatedTotal += component.getPostMitigationAmount();

        }

        result.setFinalEventDamage(finalMitigatedTotal);
        phaseState.markDefenseCalculatedAndLocked();
    }

    void handoff(DamageApplicationPlan plan) {
        if (plan.definitionGeneration() != definitions.generation())
            throw new IllegalArgumentException("Application plan belongs to another generation");
        if (plan.cancelled()) phaseState.markDefenseCalculatedAndLocked();
        if (!phaseState.defenseCalculated()) throw new IllegalStateException("Defense has not been calculated");
        eventWriter.applyIncomingDamage(this, plan);
    }

    public boolean suppressesDefaultCritical() {
        return sourceContext.suppressesDefaultCritical();
    }

    public boolean isVanillaMaceSmash() {
        return sourceContext.isVanillaMaceSmash();
    }

    public boolean isVanillaSpearAttack() {
        return sourceContext.isVanillaSpearAttack();
    }

    /*
     * ---------------------------------------------------------------------
     * Public simple accessors
     * ---------------------------------------------------------------------
     */

    public boolean isVanillaJumpCrit() {
        return hasCapturedMeleeCritical();
    }

    DamageAttackCategory attackCategory() {
        return sourceContext.attackCategory();
    }

    public boolean isMeleeDamage() {
        return attackCategory() == DamageAttackCategory.MELEE;
    }

    public boolean isProjectileDamage() {
        return attackCategory() == DamageAttackCategory.PROJECTILE;
    }

    boolean claimAttributeScaling() {
        return combat.claimAttributeScaling();
    }

    /** Captured final server-side CriticalHitEvent result. */
    public boolean hasCapturedMeleeCritical() {
        return isVanillaJumpCrit;
    }

    DamageEventSnapshot eventSnapshot() {
        return event;
    }

    DamageSourceContext sourceContext() {
        return sourceContext;
    }

    public boolean isManaged() {
        return isManaged;
    }

    public @Nullable LivingEntity logicalAttacker() {
        return origin.attribution().logicalAttacker();
    }

    public @Nullable Entity directEntity() {
        return origin.attribution().directEntity();
    }

    public @Nullable Entity effectOwner() {
        return origin.attribution().effectOwner();
    }

    public @Nullable LivingEntity equipmentOwner() {
        return origin.attribution().equipmentOwner();
    }

    public DamageAttribution attribution() {
        return origin.attribution();
    }

    public DamageLineage lineage() {
        return origin.lineage();
    }

    public DamageRequestKind requestKind() {
        return origin.requestKind();
    }

    public DamageOrigin origin() {
        return origin;
    }

    public Optional<NexusId> actionId() {
        return origin.actionId();
    }

    public Set<NexusId> sourceTags() {
        return origin.sourceTags();
    }

    public DamageMetadata metadata() {
        return origin.metadata();
    }

    public DamageAttributionProvenance attributionProvenance() {
        return origin.attributionProvenance();
    }

    /** Transaction-local source snapshot, initialized at most once. */
    @ApiStatus.Internal
    public List<ExternalItemRuleSnapshot> externalItemRuleSources() {
        if (externalItemRuleSources == null) {
            externalItemRuleSources = ExternalItemRuleSources.snapshot(this);
        }
        return externalItemRuleSources;
    }

    /** Pins one immutable template registry revision for this transaction. */
    @ApiStatus.Internal
    public DamageTemplateSnapshot templateSnapshot() {
        return templateSnapshot;
    }

    /** Pins one channel-compatible global-rule snapshot for this transaction. */
    @ApiStatus.Internal
    public DatapackDamageRuleStore.Snapshot datapackRuleSnapshot() {
        return datapackRuleSnapshot;
    }

    public LivingEntity victim() {
        return victim;
    }

    public DamageSource source() {
        return source;
    }

    public long damageId() {
        return damageId;
    }

    public DamageChannel getInitialChannel() {
        return sourceContext.initialChannel();
    }

    @Override
    public boolean hasActiveDamageInChannel(DamageChannel channel) {
        if (channel == null) {
            return false;
        }

        for (int i = 0; i < packet.activeComponentCount(); i++) {
            DamageComponent component = packet.activeComponent(i);

            if (component.channel.id().equals(channel.id())
                    && component.hasAnyPositiveDamage()) {
                return true;
            }
        }

        return false;
    }

    public boolean shouldRebuildVanillaOffensiveMobEffects() {
        return sourceContext.shouldRebuildVanillaOffensiveMobEffects();
    }

    public boolean shouldRebuildVanillaOffensiveEnchantment() {
        return sourceContext.shouldRebuildVanillaOffensiveEnchantment();
    }

    public boolean shouldRebuildVanillaPreEventDelta() {
        return sourceContext.shouldRebuildVanillaPreEventDelta();
    }

    public float getVanillaOffensiveMobEffectDelta() {
        return sourceContext.vanillaOffensiveMobEffectDelta();
    }

    public DamageApplicationBucket getInitialBaseBucket() {
        return sourceContext.initialBaseBucket();
    }

    public DamageApplicationBucket getVanillaOffensiveMobEffectBucket() {
        return sourceContext.vanillaOffensiveMobEffectBucket();
    }

    public DamageApplicationBucket getVanillaOffensiveEnchantmentBucket() {
        return sourceContext.vanillaOffensiveEnchantmentBucket();
    }

    public DamageContributionCollector contributions() {
        return contributions;
    }

    /**
     * Captures all framework-owned mutable calculation state that an
     * extension callback can reach. Runtime entity/world side effects are
     * deliberately outside this transaction boundary.
     */
    @ApiStatus.Internal
    public DamageCheckpoint checkpointMutableState() {
        return new DamageCheckpoint(
                packet.checkpoint(),
                combat.checkpoint(),
                result.checkpoint(),
                phaseState.checkpoint(),
                contributions.checkpoint(),
                beforeSteps.size()
        );
    }

    /** Restores a callback checkpoint after a tolerant callback failure. */
    @ApiStatus.Internal
    public void rollbackMutableState(DamageCheckpoint checkpoint) {
        Objects.requireNonNull(checkpoint, "Damage context checkpoint");
        packet.restore(checkpoint.packet());
        combat.restore(checkpoint.combat());
        result.restore(checkpoint.result());
        phaseState.restore(checkpoint.phase());
        contributions.rollback(checkpoint.contributionCount());
        beforeSteps.subList(checkpoint.beforeStepCount(), beforeSteps.size()).clear();
    }

    NativeDamageRuleContext restrictedView() {
        return restrictedView;
    }

    @ApiStatus.Internal
    public NativeDamageContextView restrictedContextView() {
        return restrictedView;
    }

    public float penetratedArmor() {
        return (float) io.github.fatalitaya.combatnexus.formula.PenetrationFormula.calculate(
                defenseInput(io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput.ARMOR),
                getLogicalAttackerAttrOrZero(io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes.armorPenetrationRatio()),
                getLogicalAttackerAttrOrZero(io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes.armorPenetrationFlat())).effectiveArmor();
    }

    private java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.action.CombatAction> capturedAction() {
        var session=input(victim.getUUID()).sessionId();
        var launched=projectileLaunch();
        if(launched.isPresent()) {
            var launch=launched.orElseThrow();
            return java.util.Optional.of(new io.github.fatalitaya.combatnexus.combat.api.action.CombatAction(launch.action(),session,launch.shooter().entity()));
        }
        return logicalAttacker()==null ? java.util.Optional.empty()
                : io.github.fatalitaya.combatnexus.combat.spi.CombatActionAccess.current(session,logicalAttacker().getUUID());
    }

    DamageApplicationPlan resolvedSummary(io.github.fatalitaya.combatnexus.combat.driver.api.ResolvedDamage values) {
        planFrozen = true;
        phaseState.markDefenseCalculatedAndLocked();
        return new DamageApplicationPlan(values.amount(), values.channels(), false,
                io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecisionSnapshot.unresolved(), false, null,
                new io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs(inputFrames, source.typeHolder().unwrapKey().map(k -> ContentAccess.get().keyId(k)), ContentAccess.get().damageTags(source.typeHolder())),
                Float.NaN, values.amount(), definitions.generation(), input(victim.getUUID()).behaviors(),
                capturedAction(),
                io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.EMPTY);
    }

    DamageApplicationPlan executionSummary() {
        planFrozen = true;
        java.util.Map<io.github.fatalitaya.combatnexus.api.id.NexusId, Float> channels =
                new java.util.LinkedHashMap<>();
        for (int i = 0; i < packet.activeComponentCount(); i++) {
            DamageComponent component = packet.activeComponent(i);
            float amount = Math.max(
                    0.0f,
                    component.getPostMitigationAmount()
            );
            channels.merge(
                    component.channel.id(),
                    amount,
                    Float::sum
            );
        }

        float resolved = result.finalEventDamage();
        // Preserve component proportions through final overrides. Shield reservation scales
        // this basis to the final native event amount, including later event changes.

        return new DamageApplicationPlan(
                resolved,
                channels,
                combat.critical(),
                combat.criticalDecision().snapshot(),
                result.damageCancelled(),
                result.cancelSourceId(),
                new io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs(inputFrames, source.typeHolder().unwrapKey().map(k -> ContentAccess.get().keyId(k)), ContentAccess.get().damageTags(source.typeHolder())),
                penetratedArmor(), result.offensiveTotal(), definitions.generation(), input(victim.getUUID()).behaviors(),
                capturedAction(),
                new io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan(beforeSteps)
        );
    }

}

