package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.fatalitaya.combatnexus.api.codec.CodecBudgets;
import com.mojang.serialization.DynamicOps;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDisplay;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

import static io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleLimits.*;

final class RuleStructure {
    private RuleStructure() {}
    static Optional<Integer> countConditionNodes(
            List<DamageRuleCondition> roots
    ) {
        Deque<DamageRuleCondition> pending = new ArrayDeque<>();

        for (int index = roots.size() - 1; index >= 0; index--) {
            pending.push(roots.get(index));
        }

        int count = 0;

        while (!pending.isEmpty()) {
            DamageRuleCondition condition = pending.pop();
            count++;

            List<DamageRuleCondition> children;
            try {
                children = children(condition);
            } catch (RuntimeException exception) {
                return Optional.empty();
            }
            if (children == null
                    || children.size() > MAX_RULE_CONDITIONS
                    || children.stream().anyMatch(java.util.Objects::isNull)) {
                return Optional.empty();
            }

            for (int index = children.size() - 1; index >= 0; index--) {
                pending.push(children.get(index));
            }
        }

        return Optional.of(count);
    }

    static Optional<String> findConditionGraphProblem(
            List<DamageRuleCondition> roots
    ) {
        IdentityHashMap<DamageRuleCondition, VisitState> states =
                new IdentityHashMap<>();
        Deque<ConditionFrame> stack = new ArrayDeque<>();
        int nodeCount = 0;

        for (int index = roots.size() - 1; index >= 0; index--) {
            stack.push(new ConditionFrame(roots.get(index), 1, false));
        }

        while (!stack.isEmpty()) {
            ConditionFrame frame = stack.pop();
            DamageRuleCondition condition = frame.condition();

            if (condition == null) {
                return Optional.of("null_condition");
            }

            if (frame.exiting()) {
                states.remove(condition);
                continue;
            }

            VisitState state = states.get(condition);

            if (state == VisitState.VISITING) {
                return Optional.of(
                        "condition_cycle type=" + safeConditionType(condition)
                );
            }

            if (frame.depth() > MAX_CONDITION_DEPTH) {
                return Optional.of(
                        "condition_depth=" + frame.depth()
                                + " maximum=" + MAX_CONDITION_DEPTH
                );
            }

            nodeCount++;

            if (nodeCount > MAX_CONDITION_NODES) {
                return Optional.of(
                        "condition_nodes=" + nodeCount
                                + " maximum=" + MAX_CONDITION_NODES
                );
            }

            if (condition instanceof ChannelReferencingCondition references) {
                List<io.github.fatalitaya.combatnexus.api.id.NexusId> channels;

                try {
                    channels = references.referencedChannels();
                } catch (Exception exception) {
                    return Optional.of(
                            "condition_channel_reference_failed type="
                                    + safeConditionType(condition)
                    );
                }

                if (channels == null) {
                    return Optional.of(
                            "condition_channels_are_null type="
                                    + safeConditionType(condition)
                    );
                }

                if (channels.size() > MAX_REFERENCED_CHANNELS_PER_NODE) {
                    return Optional.of(
                            "condition_channel_count=" + channels.size()
                                    + " maximum="
                                    + MAX_REFERENCED_CHANNELS_PER_NODE
                    );
                }

                if (channels.stream().anyMatch(java.util.Objects::isNull)) {
                    return Optional.of(
                            "condition_has_null_channel type="
                                    + safeConditionType(condition)
                    );
                }
            }

            List<DamageRuleCondition> children;
            try {
                children = children(condition);
            } catch (RuntimeException exception) {
                return Optional.of(
                        "condition_children_failed type="
                                + safeConditionType(condition)
                );
            }

            if (children == null) {
                return Optional.of(
                        "condition_children_are_null type="
                                + safeConditionType(condition)
                );
            }

            if (children.size() > MAX_RULE_CONDITIONS) {
                return Optional.of(
                        "condition_child_count=" + children.size()
                                + " maximum=" + MAX_RULE_CONDITIONS
                );
            }

            if (children.stream().anyMatch(java.util.Objects::isNull)) {
                return Optional.of(
                        "condition_has_null_child type="
                                + safeConditionType(condition)
                );
            }

            states.put(condition, VisitState.VISITING);
            stack.push(new ConditionFrame(
                    condition,
                    frame.depth(),
                    true
            ));

            for (int index = children.size() - 1; index >= 0; index--) {
                stack.push(new ConditionFrame(
                        children.get(index),
                        frame.depth() + 1,
                        false
                ));
            }
        }

        return Optional.empty();
    }

    static List<DamageRuleCondition> children(
            DamageRuleCondition condition
    ) {
        if (condition instanceof CompositeDamageRuleCondition composite) {
            return composite.childConditions();
        }

        return List.of();
    }

    static Optional<String> operationReferenceProblem(
            DamageRuleOperation operation
    ) {
        if (operation instanceof ChannelReferencingOperation references) {
            List<io.github.fatalitaya.combatnexus.api.id.NexusId> channels;

            try {
                channels = references.referencedChannels();
            } catch (Exception exception) {
                return Optional.of(
                        "operation_channel_reference_failed type="
                                + safeOperationType(operation)
                );
            }

            Optional<String> problem = referenceListProblem(
                    channels,
                    "operation_channel"
            );

            if (problem.isPresent()) {
                return problem;
            }
        }

        if (operation instanceof PreMultiplierBucketReferencingOperation
                references) {
            List<io.github.fatalitaya.combatnexus.api.id.NexusId> buckets;

            try {
                buckets =
                        references.referencedPreMultiplierBuckets();
            } catch (Exception exception) {
                return Optional.of(
                        "operation_bucket_reference_failed type="
                                + safeOperationType(operation)
                );
            }

            return referenceListProblem(
                    buckets,
                    "operation_bucket"
            );
        }

        return Optional.empty();
    }

    static Optional<String> referenceListProblem(
            List<io.github.fatalitaya.combatnexus.api.id.NexusId> references,
            String category
    ) {
        if (references == null) {
            return Optional.of(category + "_references_are_null");
        }

        if (references.size() > MAX_REFERENCED_CHANNELS_PER_NODE) {
            return Optional.of(
                    category + "_reference_count=" + references.size()
                            + " maximum="
                            + MAX_REFERENCED_CHANNELS_PER_NODE
            );
        }

        if (references.stream().anyMatch(java.util.Objects::isNull)) {
            return Optional.of(category + "_has_null_reference");
        }

        return Optional.empty();
    }

    static String safeConditionType(
            DamageRuleCondition condition
    ) {
        try {
            return String.valueOf(condition.type());
        } catch (Exception exception) {
            return "<unknown_condition>";
        }
    }

    static String safeOperationType(
            DamageRuleOperation operation
    ) {
        try {
            return String.valueOf(operation.type());
        } catch (Exception exception) {
            return "<unknown_operation>";
        }
    }

    private enum VisitState {
        VISITING
    }

    private record ConditionFrame(
            DamageRuleCondition condition,
            int depth,
            boolean exiting
    ) {
    }
}
