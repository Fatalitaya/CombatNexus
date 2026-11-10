package io.github.fatalitaya.combatnexus.combat.damage.core.rule;

import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.CompositeDamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleReferenceValidator;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleLimits;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleStacking;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleValidator;
import io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition.DamageChannelIsCondition;
import io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation.*;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.*;

public final class DatapackDamageRuleReloadListener {
    public static final int MAX_DATAPACK_RULES = 512;
    public static final int MAX_DATAPACK_CONDITION_NODES = 8_192;
    public static final int MAX_DATAPACK_OPERATIONS = 4_096;

    static boolean validateRule(
            NexusId fileId,
            DamageRuleDefinition rule
    ) {
        String source = "datapack/file/" + fileId;

        if (!DamageRuleValidator.validate(
                rule,
                source,
                DamageRuleValidator.Policy.IGNORE
        )) {
            return false;
        }

        boolean accepted = true;

        Deque<DamageRuleCondition> pending =
                new ArrayDeque<>(rule.conditions());

        while (!pending.isEmpty()) {
            DamageRuleCondition condition = pending.pop();
            validateCondition(fileId, rule, condition);

            if (condition instanceof CompositeDamageRuleCondition composite) {
                List<DamageRuleCondition> children = composite.childConditions();
                if (children == null
                        || children.size() > DamageRuleLimits.MAX_RULE_CONDITIONS
                        || children.stream().anyMatch(java.util.Objects::isNull)) {
                    throw new IllegalArgumentException(
                            "Invalid composite condition child list in "
                                    + fileId
                    );
                }
                pushConditions(pending, children);
            }
        }

        for (DamageRuleOperation operation : rule.operations()) {
            if (!validateOperation(fileId, rule, operation)) {
                accepted = false;
            }
        }

        return accepted;
    }

    private static boolean validateOperation(
            NexusId fileId,
            DamageRuleDefinition rule,
            DamageRuleOperation operation
    ) {
        boolean accepted = true;

        if (operation instanceof AddBaseDamageOperation addBase) {
            validateChannelId(
                    fileId,
                    rule,
                    "operation=" + operation.type(),
                    addBase.channelId()
            );
        }

        if (operation instanceof AddChannelPreMultiplierOperation addPre) {
            validateChannelId(
                    fileId,
                    rule,
                    "operation=" + operation.type(),
                    addPre.channelId()
            );

            if (!validateBucketId(
                    fileId,
                    rule,
                    operation,
                    addPre.preMultiplierBucketId()
            )) {
                accepted = false;
            }
        }

        if (operation instanceof AddChannelPostMultiplierOperation addPost) {
            validateChannelId(
                    fileId,
                    rule,
                    "operation=" + operation.type(),
                    addPost.channelId()
            );
        }

        if (operation instanceof AddTemporaryResistanceOperation addResistance) {
            validateChannelId(
                    fileId,
                    rule,
                    "operation=" + operation.type(),
                    addResistance.channelId()
            );
        }

        if (operation instanceof AddGlobalPreMultiplierOperation addGlobalPre) {
            if (!validateBucketId(
                    fileId,
                    rule,
                    operation,
                    addGlobalPre.preMultiplierBucketId()
            )) {
                accepted = false;
            }
        }

        return accepted;
    }

    private static void validateCondition(
            NexusId fileId,
            DamageRuleDefinition rule,
            DamageRuleCondition condition
    ) {
        if (condition instanceof DamageChannelIsCondition channelCondition) {
            validateChannelId(
                    fileId,
                    rule,
                    "condition=" + condition.type(),
                    channelCondition.channelId()
            );
        }
    }

    private static void pushConditions(
            Deque<DamageRuleCondition> pending,
            List<DamageRuleCondition> conditions
    ) {
        for (int index = conditions.size() - 1; index >= 0; index--) {
            pending.push(conditions.get(index));
        }
    }

    private static void validateChannelId(
            NexusId fileId,
            DamageRuleDefinition rule,
            String location,
            NexusId channelId
    ) {
        /* CHANNELS -> GLOBAL_RULES is an explicit reload dependency. The later
         * strict reference pass rejects this rule; this message only supplies
         * diagnostic context and never promises an untyped fallback. */
    }

    private static boolean validateBucketId(
            NexusId fileId,
            DamageRuleDefinition rule,
            DamageRuleOperation operation,
            Optional<NexusId> bucketId
    ) {
        if (bucketId.isEmpty()) {
            return true;
        }

        NexusId id = bucketId.get();

        /*
         * Fatal:
         * unknown pre-multiplier bucket id will throw at runtime when the operation applies.
         */
        if (!PreMultiplierBucketRegistry.containsPreMultiplierBucket(id)) {
            return false;
        }

        return true;
    }

    private static boolean requiresStackingGroup(DamageRuleStacking stacking) {
        return switch (stacking) {
            case STACK -> false;
            case UNIQUE_SOURCE,
                 HIGHEST_VALUE,
                 LOWEST_VALUE,
                 REPLACE -> true;
        };
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean applyPreparedForTesting(
            Map<NexusId, DamageRuleDefinition> prepared
    ) {
        return publishPrepared(prepared);
    }

    private static boolean publishPrepared(Map<NexusId, DamageRuleDefinition> prepared) {
        try {
            var candidate = buildCandidate(prepared);
            DatapackDamageRuleStore.replace(candidate.rules(), candidate.validatedChannelRevision());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static DatapackDamageRuleStore.Snapshot buildCandidate(
            Map<NexusId, DamageRuleDefinition> prepared) {
        findAggregateProblem(prepared).ifPresent(problem -> { throw new IllegalArgumentException(problem); });
        var entries = new ArrayList<>(prepared.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        var accepted = new ArrayList<DamageRuleDefinition>();
        var ids = new java.util.HashSet<NexusId>();
        for (var entry : entries) {
            var rule = entry.getValue();
            if (!ids.add(rule.id()) || !validateRule(entry.getKey(), rule)
                    || !DamageRuleReferenceValidator.validateDatapackReferences(
                            rule, "datapack/reference", DamageRuleValidator.Policy.IGNORE)) {
                throw new IllegalArgumentException("Invalid global damage rule: " + entry.getKey());
            }
            accepted.add(rule);
        }
        return DatapackDamageRuleStore.Snapshot.create(
                DamageChannelRegistry.contentRevision(),
                DamageChannelRegistry.contentRevision(), true, accepted);
    }

    static Optional<String> findAggregateProblem(
            Map<NexusId, DamageRuleDefinition> prepared
    ) {
        if (prepared == null) {
            return Optional.of("prepared_rules_are_null");
        }

        if (prepared.size() > MAX_DATAPACK_RULES) {
            return Optional.of(
                    "datapack_rule_count=" + prepared.size()
                            + " maximum=" + MAX_DATAPACK_RULES
            );
        }

        long conditionNodes = 0L;
        long operations = 0L;

        for (DamageRuleDefinition rule : prepared.values()) {
            Optional<DamageRuleLimits.RuleCost> cost =
                    DamageRuleLimits.measureRuleCost(rule);

            if (cost.isEmpty()) {
                return Optional.of("datapack_rule_is_not_measurable");
            }

            if (conditionNodes
                    > MAX_DATAPACK_CONDITION_NODES
                    - (long) cost.get().conditionNodes()) {
                return Optional.of(
                        "datapack_condition_nodes="
                                + (conditionNodes
                                + cost.get().conditionNodes())
                                + " maximum="
                                + MAX_DATAPACK_CONDITION_NODES
                );
            }

            conditionNodes += cost.get().conditionNodes();

            if (operations
                    > MAX_DATAPACK_OPERATIONS
                    - (long) cost.get().operations()) {
                return Optional.of(
                        "datapack_operations="
                                + (operations
                                + cost.get().operations())
                                + " maximum="
                                + MAX_DATAPACK_OPERATIONS
                );
            }

            operations += cost.get().operations();
        }

        return Optional.empty();
    }

}

