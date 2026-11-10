package io.github.fatalitaya.combatnexus.combat.damage.core.settlement;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatCompletionQueue;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageFailureReason;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementStatus;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageApplicationPlan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Map;

/**
 * Framework-owned settlement state and the sole ordinary caller authorized to
 * complete a public settlement snapshot.
 */
@ApiStatus.Internal
public final class DamageSettlementState {
    private boolean applicationCompleted;
    private boolean contactAccepted;
    private boolean beforePlanEntered;
    private io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result beforeResult = io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result.EMPTY;
    void appendBeforeResult(io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result result) {
        var steps=new java.util.ArrayList<>(beforeResult.steps());
        int offset=steps.size();
        for(var step:result.steps()) steps.add(new io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.StepEvidence(offset+step.index(),step.commandType(),step.result()));
        beforeResult = new io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result(steps,
                beforeResult.continueParent() && result.continueParent(), beforeResult.completed() && result.completed());
    }
    boolean applyBeforePlan() {
        if (beforePlanEntered) return beforeResult.continueParent();
        if (!beforeResult.continueParent()) return false;
        if (calculation == null || calculation.cancelled()) return true;
        beforePlanEntered = true;
        var server = CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer()));
        try (var applying = server.ticket(origin.lineage().transaction()).enter(
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.APPLICATION)) {
            appendBeforeResult(io.github.fatalitaya.combatnexus.combat.internal.transaction.BeforeApplicationExecutor.execute(
                    server.world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession()).orElseThrow(), calculation.beforeApplication()));
        } catch (RuntimeException | Error failure) {
            appendBeforeResult(new io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result(java.util.List.of(), false, false));
            throw failure;
        }
        return beforeResult.continueParent() && target.isAlive() && !target.isRemoved() && target.level() == level;
    }
    private boolean ambiguousHealthWrite;
    private boolean healthWriteObserved;
    private float confirmedHealthLoss;
    private double shieldLoss;
    private io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockReceipt itemBlock;
    void recordItemBlock(io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockReceipt receipt) {
        requirePhase(DamageSettlementPhase.CALCULATED, "record item block");
        if (itemBlock != null) throw new IllegalStateException("Duplicate item block");
        itemBlock = Objects.requireNonNull(receipt);
    }
    private final java.util.List<io.github.fatalitaya.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt> equipment = new java.util.ArrayList<>();
    void recordEquipmentReceipt(io.github.fatalitaya.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt receipt) {
        equipment.add(java.util.Objects.requireNonNull(receipt));
    }
    private boolean equipmentConsumed() { return equipment.stream().anyMatch(receipt -> receipt.durabilityConsumed() > 0); }
    private final java.util.List<io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry> writes = new java.util.ArrayList<>();
    private boolean intercepted;
    private final java.util.EnumSet<io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardBehavior.Stage> guardStages =
            java.util.EnumSet.noneOf(io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardBehavior.Stage.class);
    boolean guard(io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardBehavior.Stage stage, float damage, float remaining) {
        if (calculation == null || calculation.cancelled() || lateIncomingCancellation || damage <= 0
                || !guardStages.add(stage)) return intercepted;
        if (io.github.fatalitaya.combatnexus.combat.driver.internal.ResolvedNumbers.driver(origin.lineage().damageId())
                .filter(driver -> driver.managedProtection() == io.github.fatalitaya.combatnexus.combat.driver.api.DriverDefinition.ApplicationOwner.EXTERNAL).isPresent()) return false;
        var server = CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) level.getServer());
        var world = server.world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) level).combatnexus$worldSession()).orElseThrow();
        try (var applying = server.ticket(origin.lineage().transaction()).enter(
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.APPLICATION)) {
            var context = new io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardContext(target.getUUID(), stage,
                    origin.requestKind(), origin.sourceTags(), calculation.action(), target.getHealth(), target.getMaxHealth(),
                    damage, remaining, calculation.reactionInputs(), null);
            var decision = world.behaviorControl().guard(context, calculation.behaviors());
            if (!decision.selected()) return false;
            decision.consumption().ifPresent(writes::add);
            intercepted |= stage == io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardBehavior.Stage.BEFORE_RESOURCES;
            try {
                var result = io.github.fatalitaya.combatnexus.combat.internal.transaction.BeforeApplicationExecutor.execute(world, decision.plan());
                appendBeforeResult(result);
                if (!result.continueParent() || !result.completed()) throw new IllegalStateException("Damage guard application stopped");
                server.ticket(origin.lineage().transaction()).bindTarget(world, target.getUUID());
                if (!target.isAlive() || target.isRemoved() || target.level() != level) throw new IllegalStateException("Damage guard target unavailable");
            } catch (RuntimeException | Error failure) {
                appendBeforeResult(new io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result(java.util.List.of(), false, false));
                throw failure;
            }
            return intercepted;
        }
    }
    private boolean protectionDecided;
    private Float protectedHealth;
    float protectHealth(float before, float proposed) {
        if (io.github.fatalitaya.combatnexus.combat.driver.internal.ResolvedNumbers.driver(origin.lineage().damageId())
                .filter(driver -> driver.managedProtection() == io.github.fatalitaya.combatnexus.combat.driver.api.DriverDefinition.ApplicationOwner.EXTERNAL).isPresent()) return proposed;
        if (before > 0 && Float.isFinite(proposed) && proposed < before)
            proposed = Math.max(proposed, Math.min(before, origin.minimumHealth()));
        if (protectionDecided) return protectedHealth == null ? proposed : Math.max(proposed, protectedHealth);
        protectionDecided = true;
        if (calculation == null || before <= 0 || !Float.isFinite(proposed) || proposed > 0 || proposed >= before) return proposed;
        var server = CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer()));
        var world = server.world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession()).orElseThrow();
        try (var applying = server.ticket(origin.lineage().transaction()).enter(
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.APPLICATION)) {
            var decision = io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.protect(
                    world, target.getUUID(), calculation.behaviors(), before, proposed, calculation.reactionInputs(), java.util.Optional.of(new io.github.fatalitaya.combatnexus.combat.api.CombatSource(origin.source().damageType(), java.util.Optional.ofNullable(origin.attribution().logicalAttacker()).map(net.minecraft.world.entity.Entity::getUUID), java.util.Optional.empty())));
            if (decision.consumption().isPresent()) {
                writes.add(decision.consumption().orElseThrow());
                protectedHealth = decision.health();
                return protectedHealth;
            }
        }
        return proposed;
    }
    void recordProtection(java.util.List<io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry> entries) {
        writes.addAll(entries);
    }
    boolean permitsNativeHitEffects() {
        if (calculation == null) return true;
        return !calculation.cancelled() && !lateIncomingCancellation && beforeResult.completed()
                && (confirmedHealthLoss > 0 || shieldLoss > 0 || equipmentConsumed() || positiveDelta(absorptionBefore, target.getAbsorptionAmount()) > 0);
    }
    private int pendingHealthWrite = -1;
    void beginHealthWrite(float before) {
        pendingHealthWrite = writes.size();
        writes.add(new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry(
                io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH, java.util.Optional.empty(),
                java.util.OptionalDouble.of(before), java.util.OptionalDouble.empty(), 0,
                io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Observation.UNKNOWN, false));
    }
    void recordHealthWrite(float before, float after, boolean completed, boolean attributable) {
        if (!attributable) {
            ambiguousHealthWrite = true;
            if (pendingHealthWrite >= 0) { pendingHealthWrite = -1; }
            return; // The preallocated UNKNOWN entry remains; net delta cannot prove this write.
        }
        healthWriteObserved = true;
        confirmedHealthLoss += positiveDelta(before, after);
        var entry = io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry.write(
                io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH, before, after, positiveDelta(before, after), completed);
        if (pendingHealthWrite >= 0) { writes.set(pendingHealthWrite, entry); pendingHealthWrite = -1; }
        else writes.add(entry);
    }
    private io.github.fatalitaya.combatnexus.combat.shield.api.ShieldCommitReceipt shieldReceipt;
    void recordShieldReceipt(io.github.fatalitaya.combatnexus.combat.shield.api.ShieldCommitReceipt receipt) {
        if (receipt.applicationId() != origin.lineage().damageId() || !receipt.target().equals(target.getUUID()))
            throw new IllegalArgumentException("Shield receipt belongs to another application");
        if (shieldReceipt != null) {
            if (!shieldReceipt.equals(receipt)) throw new IllegalStateException("Conflicting shield commit receipts");
            return;
        }
        shieldReceipt = receipt;
        shieldLoss += receipt.absorbed(); writes.addAll(receipt.writes());
    }

    private final DamageOrigin origin;
    private final LivingEntity target;
    private final ServerLevel level;
    private final float incomingHealth;
    private final float incomingAbsorption;

    private DamageSettlementPhase phase = DamageSettlementPhase.OPEN;
    private DamageApplicationPlan calculation;
    DamageApplicationPlan calculation() { return calculation; }
    private float healthBefore;
    private float absorptionBefore;
    private boolean lateIncomingCancellation;
    private DamageSettlementCompletion completion;
    private final io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatCompletionQueue.Slot notification;
    private boolean notificationQueued;

    void enqueue(Runnable delivery) {
        if (notificationQueued) return;
        if (notification != null) notification.publish(delivery);
        notificationQueued = true;
    }

    DamageSettlementState(
            DamageOrigin origin,
            LivingEntity target,
            ServerLevel level
    ) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.target = Objects.requireNonNull(target, "target");
        this.level = Objects.requireNonNull(level, "level");
        var kernel = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer()));
        this.notification = kernel.hasTicket(origin.lineage().transaction())
                ? kernel.reserveCompletion(origin.lineage().transaction()) : kernel.rejectionCompletion().orElse(null);
        this.incomingHealth = safeAmount(target.getHealth());
        this.incomingAbsorption = safeAmount(target.getAbsorptionAmount());
        this.healthBefore = incomingHealth;
        this.absorptionBefore = incomingAbsorption;
    }

    io.github.fatalitaya.combatnexus.combat.api.transaction.CombatLineage lineage() { return origin.lineage().transaction(); }

    DamageSettlementPhase phase() {
        return phase;
    }

    void calculated(DamageApplicationPlan summary) {
        requirePhase(DamageSettlementPhase.OPEN, "calculate");
        this.calculation = Objects.requireNonNull(summary, "summary");
        this.phase = DamageSettlementPhase.CALCULATED;
    }

    void rejectAdmission(DamageFailureReason reason) {
        requirePhase(DamageSettlementPhase.OPEN, "reject admission for");
        Objects.requireNonNull(reason, "reason");
        phase = DamageSettlementPhase.ADMISSION_REJECTED;
        completion = new DamageSettlementCompletion(
                DamageSettlementSnapshot.completeInternal(
                        origin,
                        target,
                        level,
                        Map.of(),
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        incomingHealth,
                        incomingHealth,
                        incomingAbsorption,
                        incomingAbsorption,
                        false,
                        true,
                        false,
                        DamageSettlementStatus.NOT_APPLIED,
                        reason,
                        null,
                        io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageApplicationEvidence.NONE
                ),
                this
        );
    }

    void markLateIncomingCancellation() {
        if (phase == DamageSettlementPhase.CALCULATED) {
            lateIncomingCancellation = true;
        }
    }

    void capturePre(float health, float absorption) {
        requirePhase(DamageSettlementPhase.CALCULATED, "capture pre-apply");
        healthBefore = safeAmount(health);
        absorptionBefore = safeAmount(absorption);
        phase = DamageSettlementPhase.PRE_APPLY_CAPTURED;
        contactAccepted = true;
    }

    DamageSettlementCompletion capturePost(float inflictedDamage, float observedHealth, float observedAbsorption) {
        requirePhase(
                DamageSettlementPhase.PRE_APPLY_CAPTURED,
                "capture post-apply"
        );

        applicationCompleted = !ambiguousHealthWrite && beforeResult.completed();
        float inflicted = safeAmount(inflictedDamage);
        float healthAfter = safeAmount(observedHealth);
        float absorptionAfter = safeAmount(observedAbsorption);

        float actualHealthDamage = positiveDelta(
                healthBefore,
                healthAfter
        );
        float actualAbsorptionDamage = positiveDelta(
                absorptionBefore,
                absorptionAfter
        );

        writes.add(new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry(
                io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Resource.ABSORPTION, java.util.Optional.empty(),
                java.util.OptionalDouble.of(absorptionBefore), java.util.OptionalDouble.of(absorptionAfter),
                actualAbsorptionDamage, io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Observation.WHOLE_CALL, true));
        DamageFailureReason reason = terminalReason(inflicted + actualAbsorptionDamage, false);
        DamageSettlementStatus status = reason == null
                ? DamageSettlementStatus.APPLIED
                : DamageSettlementStatus.NOT_APPLIED;
        phase = status == DamageSettlementStatus.APPLIED
                ? DamageSettlementPhase.APPLIED
                : DamageSettlementPhase.NOT_APPLIED;

        completion = new DamageSettlementCompletion(
                snapshot(
                        status,
                        reason,
                        inflicted,
                        actualHealthDamage,
                        actualAbsorptionDamage,
                        healthAfter,
                        absorptionAfter,
                        calculation.cancelled()
                ),
                this
        );
        return completion;
    }

    DamageSettlementCompletion completeWithoutPost(boolean vanillaAccepted, boolean incomingCancelled) {
        if (itemBlock != null) itemBlock = new io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockReceipt(
                itemBlock.item(), itemBlock.hand(), itemBlock.incoming(), itemBlock.blocked(), itemBlock.remaining(), true);
        if (phase == DamageSettlementPhase.ADMISSION_REJECTED) {
            return completion;
        }
        if (phase == DamageSettlementPhase.APPLIED
                || phase == DamageSettlementPhase.NOT_APPLIED) {
            // In 1.21.1 Post precedes totem/death handling inside hurt. Preserve the
            // Post resource deltas, but freeze the journal only after hurt returns.
            var post = completion.snapshot();
            completion = new DamageSettlementCompletion(snapshot(post.status(), post.reason().orElse(null),
                    post.appliedDamage(), post.healthDamage(), post.absorptionDamage(),
                    post.healthAfter(), post.absorptionAfter(), post.cancelled()), this);
            return completion;
        }

        if (phase != DamageSettlementPhase.CALCULATED
                && phase != DamageSettlementPhase.PRE_APPLY_CAPTURED) {
            return null;
        }

        if (contactAccepted && !incomingCancelled && !lateIncomingCancellation && !calculation.cancelled()
                && beforeResult.completed() && (calculation.resolvedDamage() == 0 || intercepted)) {
            return capturePost(0, target.getHealth(), target.getAbsorptionAmount());
        }
        if (vanillaAccepted) {
            return abort();
        }

        if (incomingCancelled) {
            lateIncomingCancellation = true;
        }
        DamageFailureReason reason = terminalReason(0.0f, true);
        if (reason == null) {
            reason = DamageFailureReason.VANILLA_REJECTED;
        }

        phase = DamageSettlementPhase.NOT_APPLIED;
        completion = new DamageSettlementCompletion(
                snapshot(
                        DamageSettlementStatus.NOT_APPLIED,
                        reason,
                        0.0f,
                        0.0f,
                        0.0f,
                        safeAmount(target.getHealth()),
                        safeAmount(target.getAbsorptionAmount()),
                        calculation.cancelled()
                                || lateIncomingCancellation
                ),
                this
        );
        return completion;
    }

    DamageSettlementCompletion abort() {
        if (completion != null) return completion;
        if (calculation != null && (confirmedHealthLoss > 0 || shieldLoss > 0 || equipmentConsumed())) {
            // Confirmed writes survive exceptions. The evidence explicitly marks incomplete application.
            phase = DamageSettlementPhase.APPLIED;
            completion = new DamageSettlementCompletion(snapshot(DamageSettlementStatus.APPLIED, null,
                    confirmedHealthLoss, confirmedHealthLoss, 0, safeAmount(target.getHealth()),
                    safeAmount(target.getAbsorptionAmount()), false), this);
            return completion;
        }
        if (calculation == null) calculation = new DamageApplicationPlan(0, Map.of(), false, false, null, io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs.EMPTY);
        phase = DamageSettlementPhase.NOT_APPLIED;
        completion = new DamageSettlementCompletion(snapshot(DamageSettlementStatus.NOT_APPLIED,
                DamageFailureReason.INTERNAL_ERROR, 0, 0, 0, safeAmount(target.getHealth()),
                safeAmount(target.getAbsorptionAmount()), false), this);
        return completion;
    }

    void markPublished() {
        if (phase != DamageSettlementPhase.ADMISSION_REJECTED
                && phase != DamageSettlementPhase.APPLIED
                && phase != DamageSettlementPhase.NOT_APPLIED) {
            throw new IllegalStateException(
                    "Cannot publish settlement in phase " + phase
            );
        }
        phase = DamageSettlementPhase.PUBLISHED;
    }

    private DamageFailureReason terminalReason(
            float inflicted,
            boolean noPost
    ) {
        if (!beforeResult.completed()) return DamageFailureReason.INTERNAL_ERROR;
        if (calculation.cancelled()) {
            return DamageFailureReason.CANCELLED;
        }
        if (calculation.resolvedDamage() <= 0.0f) {
            return DamageFailureReason.ZERO_DAMAGE;
        }
        if (lateIncomingCancellation) {
            return DamageFailureReason.LATE_INCOMING_CANCELLATION;
        }
        if (!noPost && inflicted <= 0.0f && shieldLoss <= 0 && !equipmentConsumed()) {
            return DamageFailureReason.ZERO_AFTER_PRE;
        }
        if (noPost) {
            return DamageFailureReason.VANILLA_REJECTED;
        }
        return null;
    }

    private DamageSettlementSnapshot snapshot(
            DamageSettlementStatus status,
            DamageFailureReason reason,
            float appliedDamage,
            float healthDamage,
            float absorptionDamage,
            float healthAfter,
            float absorptionAfter,
            boolean cancelled
    ) {
        return DamageSettlementSnapshot.completeInternal(
                origin,
                target,
                level,
                calculation.resolvedChannelDamage(),
                calculation.resolvedDamage(),
                appliedDamage,
                healthDamage,
                absorptionDamage,
                healthBefore,
                healthAfter,
                absorptionBefore,
                absorptionAfter,
                calculation.critical(),
                calculation.criticalDecision(),
                cancelled,
                true,
                status,
                reason,
                calculation.cancelSourceId(),
                evidence()
        );
    }

    DamageOrigin origin() { return origin; }
    ServerLevel level() { return level; }
    io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageApplicationEvidence evidence() {
        if (calculation == null) return io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageApplicationEvidence.NONE;
        return new io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageApplicationEvidence(
                        contactAccepted, intercepted, healthWriteObserved, confirmedHealthLoss, shieldLoss, calculation.reactionInputs(), applicationCompleted,
                        new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal(writes, applicationCompleted), calculation.behaviors(), calculation.action(), beforeResult, equipment, java.util.Optional.ofNullable(itemBlock));
    }

    private void requirePhase(
            DamageSettlementPhase expected,
            String action
    ) {
        if (phase != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " settlement in phase " + phase
            );
        }
    }

    private static float safeAmount(float amount) {
        return Float.isFinite(amount) ? Math.max(0.0f, amount) : 0.0f;
    }

    private static float positiveDelta(float before, float after) {
        return safeAmount(before - after);
    }
}
