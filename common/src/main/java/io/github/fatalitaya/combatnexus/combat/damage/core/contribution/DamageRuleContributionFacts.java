package io.github.fatalitaya.combatnexus.combat.damage.core.contribution;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModulePreMultiplierBuckets;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageContributionSourceKind;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageContributionStatus;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.*;
import io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.ContributionFact;
import org.jetbrains.annotations.Nullable;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Locale;
import java.util.Optional;

public final class DamageRuleContributionFacts {

    private DamageRuleContributionFacts() {
    }

    public static @Nullable ContributionFact operation(
            long generation,
            DamagePhase runningPhase,
            RuntimeDamageRule runtimeRule,
            DamageRuleOperation operation,
            DamageMutationResult result,
            int operationIndex
    ) {
        if (runtimeRule == null
                || operation == null
                || result == null) {
            return null;
        }

        var rule = runtimeRule.definition();
        var owner = runtimeRule.owner();
        var exec = runtimeRule.executionContext();
        var details = new ContributionFact.RuleDetails(
                rule.id(), operation.type(), owner.kind(), owner.affixId(), owner.effectiveEntryId(),
                exec == null ? null : exec.providerType(), exec == null ? null : exec.sourceLocation(),
                exec == null ? null : exec.role(), result, Optional.ofNullable(exec == null ? null : exec.contributionSource()));
        return new ContributionFact(
                generation, operationIndex, contributionId(rule.id(), operation.type(), operationIndex),
                sourceKind(owner), DamageContributionOperationKindMapper.fromRuleOperationType(operation.type()),
                runningPhase, channel(operation), applicationBucket(operation), preMultiplierBucket(operation),
                operation.stackingValue(), DamageContributionStatus.APPLIED,
                owner.displayOwnerId().isPresent() ? owner.displayOwnerId() : rule.stackingGroup(),
                owner.kind() == DamageRuleOwnerKind.AFFIX ? owner.effectiveEntryId() : Optional.empty(), details);
    }

    private static DamageContributionSourceKind sourceKind(
            DamageRuleOwner owner
    ) {
        if (owner == null || owner.kind() == null) {
            return DamageContributionSourceKind.RULE;
        }

        return switch (owner.kind()) {
            case AFFIX -> DamageContributionSourceKind.AFFIX;
            case ENTRY -> DamageContributionSourceKind.ENTRY;
            case RULE -> DamageContributionSourceKind.RULE;
        };
    }

    private static NexusId contributionId(
            NexusId ruleId,
            NexusId operationType,
            int operationIndex
    ) {
        String path = "rule/"
                + sanitize(ruleId.getNamespace())
                + "/"
                + sanitize(ruleId.getPath())
                + "/"
                + sanitize(operationType.getPath())
                + "/"
                + Math.max(0, operationIndex);

        return DamageModuleIds.id(path);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value
                .replace(':', '_')
                .replace('/', '_')
                .replace(' ', '_')
                .toLowerCase(Locale.ROOT);
    }

    private static Optional<NexusId> channel(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddBaseDamageOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelPreMultiplierOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelPostMultiplierOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddTemporaryResistanceOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelMitigationOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddTrueDamageOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof ConvertDamageOperation op) {
            return Optional.ofNullable(op.toChannel());
        }

        if (operation instanceof GainExtraDamageOperation op) {
            return Optional.ofNullable(op.toChannel());
        }

        return Optional.empty();
    }

    private static Optional<DamageApplicationBucket> applicationBucket(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddBaseDamageOperation op) {
            return Optional.ofNullable(op.applicationBucket());
        }

        if (operation instanceof AddTrueDamageOperation) {
            return Optional.of(DamageApplicationBucket.DN_TRUE_DAMAGE);
        }

        if (operation instanceof GainExtraDamageOperation) {
            return Optional.of(DamageApplicationBucket.DN_RULE_BASE);
        }

        return Optional.empty();
    }

    private static Optional<NexusId> preMultiplierBucket(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddChannelPreMultiplierOperation op) {
            return op.preMultiplierBucketId()
                    .or(() -> Optional.of(DamageModulePreMultiplierBuckets.GENERIC_DAMAGE));
        }

        if (operation instanceof AddGlobalPreMultiplierOperation op) {
            return op.preMultiplierBucketId()
                    .or(() -> Optional.of(DamageModulePreMultiplierBuckets.GENERIC_DAMAGE));
        }

        return Optional.empty();
    }
}
