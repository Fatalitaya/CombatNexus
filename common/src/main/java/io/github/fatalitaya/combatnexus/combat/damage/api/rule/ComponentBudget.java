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

final class ComponentBudget {
        private long rules;
        private long conditionNodes;
        private long operations;
        private long displayCodePoints;

        Optional<String> addEntry(
                EntryDefinition entry
        ) {
            Optional<String> displayProblem = addDisplay(entry.display());

            if (displayProblem.isPresent()) {
                return displayProblem;
            }

            for (DamageRuleDefinition rule : entry.rules()) {
                Optional<RuleCost> cost = measureRuleCost(rule);

                if (cost.isEmpty()) {
                    return Optional.of("component_rule_is_not_measurable");
                }

                Optional<String> problem = add(
                        "component_rule_count",
                        1L,
                        MAX_COMPONENT_RULES,
                        ValueKind.RULES
                );

                if (problem.isPresent()) {
                    return problem;
                }

                problem = add(
                        "component_condition_nodes",
                        cost.get().conditionNodes(),
                        MAX_COMPONENT_CONDITION_NODES,
                        ValueKind.CONDITIONS
                );

                if (problem.isPresent()) {
                    return problem;
                }

                problem = add(
                        "component_operations",
                        cost.get().operations(),
                        MAX_COMPONENT_OPERATIONS,
                        ValueKind.OPERATIONS
                );

                if (problem.isPresent()) {
                    return problem;
                }
            }

            return Optional.empty();
        }

        Optional<String> addDisplay(
                EntryDisplay display
        ) {
            return addDisplay(
                    display.name(),
                    display.authoredSummary(),
                    display.flavorText()
            );
        }

        Optional<String> addDisplay(
                io.github.fatalitaya.combatnexus.combat.api.affix
                        .AffixDisplay display
        ) {
            return addDisplay(
                    display.name(),
                    display.authoredSummary(),
                    display.flavorText()
            );
        }

        Optional<String> addDisplay(
                Optional<DisplayText> name,
                List<DisplayText> tooltip,
                Optional<DisplayText> flavor
        ) {
            return add(
                    "component_display_code_points",
                    DamageRuleLimits.displayCodePoints(
                            name,
                            tooltip,
                            flavor
                    ),
                    MAX_COMPONENT_DISPLAY_CODE_POINTS,
                    ValueKind.DISPLAY
            );
        }

        private Optional<String> add(
                String name,
                long amount,
                long maximum,
                ValueKind kind
        ) {
            long current = switch (kind) {
                case RULES -> rules;
                case CONDITIONS -> conditionNodes;
                case OPERATIONS -> operations;
                case DISPLAY -> displayCodePoints;
            };

            if (amount < 0L || current > maximum - amount) {
                return Optional.of(
                        name + "="
                                + (amount < 0L
                                ? "overflow"
                                : current + amount)
                                + " maximum=" + maximum
                );
            }

            long next = current + amount;

            switch (kind) {
                case RULES -> rules = next;
                case CONDITIONS -> conditionNodes = next;
                case OPERATIONS -> operations = next;
                case DISPLAY -> displayCodePoints = next;
            }

            return Optional.empty();
        }
        private enum ValueKind {
        RULES,
        CONDITIONS,
        OPERATIONS,
        DISPLAY
    }

}
