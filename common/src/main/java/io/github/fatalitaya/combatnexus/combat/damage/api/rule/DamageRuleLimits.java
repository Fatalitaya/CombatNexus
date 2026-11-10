package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import static io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleStructure.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import io.github.fatalitaya.combatnexus.api.codec.CodecBudgets;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;

import java.util.List;
import java.util.Optional;

/**
 * Central structural budgets for all authored, stored, synchronized and Java-created executable
 * item rules.
 */
public final class DamageRuleLimits {
    public static final int MAX_ITEM_ENTRIES = 32;
    public static final int MAX_ITEM_AFFIXES = 16;
    public static final int MAX_AFFIX_ENTRIES = 16;
    public static final int MAX_ENTRY_RULES = 32;
    public static final int MAX_RULE_CONDITIONS = 32;
    public static final int MAX_RULE_OPERATIONS = 32;
    public static final int MAX_CONDITION_DEPTH = 16;
    public static final int MAX_CONDITION_NODES = 256;
    public static final int MAX_EXPANDED_ITEM_RULES = 128;
    public static final int MAX_TOOLTIP_LINES = 32;
    public static final int MAX_TRANSLATION_ARGS =
            io.github.fatalitaya.combatnexus.presentation.api.DisplayTextLimits
                    .MAX_TRANSLATION_ARGS;
    public static final int MAX_DISPLAY_CODE_POINTS =
            io.github.fatalitaya.combatnexus.presentation.api.DisplayTextLimits
                    .MAX_DISPLAY_CODE_POINTS;
    public static final int MAX_RAW_CODEC_DEPTH = 64;
    public static final int MAX_RAW_CODEC_NODES = 4_096;
    public static final int MAX_COMPONENT_RAW_NODES = 16_384;
    public static final int MAX_COMPONENT_RULES = 128;
    public static final int MAX_COMPONENT_CONDITION_NODES = 4_096;
    public static final int MAX_COMPONENT_OPERATIONS = 2_048;
    public static final int MAX_COMPONENT_DISPLAY_CODE_POINTS = 16_384;
    public static final int MAX_REFERENCED_CHANNELS_PER_NODE = 32;
    public static final float MAX_ABSOLUTE_DAMAGE_VALUE =
            io.github.fatalitaya.combatnexus.combat.damage.api.DamageLimits.MAX_BASE_DAMAGE;
    public static final float MAX_ABSOLUTE_MULTIPLIER = 100.0f;

    private DamageRuleLimits() {}

    public static <T> Codec<List<T>> boundedList(Codec<T> codec, int maximum, String name) {
        return CodecBudgets.boundedList(codec, maximum, name);
    }

    public static Codec<String> boundedString(int maximumCodePoints, String name) {
        return CodecBudgets.boundedString(maximumCodePoints, name);
    }

    public static <A> Codec<A> guardRawStructure(Codec<A> codec, String name) {
        return guardRawStructure(codec, name, MAX_RAW_CODEC_DEPTH, MAX_RAW_CODEC_NODES);
    }

    public static <A> Codec<A> guardRawStructure(
            Codec<A> codec, String name, int maximumDepth, int maximumNodes) {
        return CodecBudgets.guardRawStructure(codec, name, maximumDepth, maximumNodes);
    }

    public static DataResult<DamageRuleDefinition> validateRuleCodec(DamageRuleDefinition rule) {
        Optional<String> problem = findRuleProblem(rule);

        return problem.<DataResult<DamageRuleDefinition>>map(
                        message ->
                                DataResult.error(
                                        () ->
                                                "Invalid DamageModule rule structure. rule="
                                                        + safeRuleId(rule)
                                                        + " reason="
                                                        + message))
                .orElseGet(() -> DataResult.success(rule));
    }

    public static Optional<String> findRuleProblem(DamageRuleDefinition rule) {
        if (rule == null) {
            return Optional.of("rule_is_null");
        }

        if (rule.conditions() == null) {
            return Optional.of("conditions_are_null");
        }

        if (rule.conditions().size() > MAX_RULE_CONDITIONS) {
            return Optional.of(
                    "condition_count="
                            + rule.conditions().size()
                            + " maximum="
                            + MAX_RULE_CONDITIONS);
        }

        if (rule.operations() == null) {
            return Optional.of("operations_are_null");
        }

        if (rule.operations().size() > MAX_RULE_OPERATIONS) {
            return Optional.of(
                    "operation_count="
                            + rule.operations().size()
                            + " maximum="
                            + MAX_RULE_OPERATIONS);
        }

        Optional<String> conditionProblem = findConditionGraphProblem(rule.conditions());

        if (conditionProblem.isPresent()) {
            return conditionProblem;
        }

        for (DamageRuleOperation operation : rule.operations()) {
            if (operation == null) {
                return Optional.of("null_operation");
            }

            Optional<String> referenceProblem = operationReferenceProblem(operation);

            if (referenceProblem.isPresent()) {
                return referenceProblem;
            }

            float value;

            try {
                value = operation.stackingValue();
            } catch (Exception exception) {
                return Optional.of(
                        "operation_stacking_value_failed type="
                                + safeOperationType(operation)
                                + " exception="
                                + exception.getClass().getSimpleName());
            }

            if (!Float.isFinite(value)) {
                return Optional.of(
                        "non_finite_operation_value type=" + safeOperationType(operation));
            }

            float maximum;
            try {
                maximum =
                        switch (operation.numericLimit()) {
                            case DAMAGE -> MAX_ABSOLUTE_DAMAGE_VALUE;
                            case MULTIPLIER -> MAX_ABSOLUTE_MULTIPLIER;
                        };
            } catch (RuntimeException failure) {
                return Optional.of(
                        "operation_numeric_limit_failed type=" + safeOperationType(operation));
            }

            if (Math.abs(value) > maximum) {
                return Optional.of(
                        "operation_value_out_of_range type="
                                + safeOperationType(operation)
                                + " value="
                                + value
                                + " maximum_absolute="
                                + maximum);
            }
        }

        return Optional.empty();
    }

    public static Optional<String> findItemProblem(
            List<EntryDefinition> entries, List<AffixDefinition> affixes) {
        List<EntryDefinition> safeEntries = entries == null ? List.of() : entries;
        List<AffixDefinition> safeAffixes = affixes == null ? List.of() : affixes;

        if (safeEntries.size() > MAX_ITEM_ENTRIES) {
            return Optional.of(
                    "item_entry_count=" + safeEntries.size() + " maximum=" + MAX_ITEM_ENTRIES);
        }

        if (safeAffixes.size() > MAX_ITEM_AFFIXES) {
            return Optional.of(
                    "item_affix_count=" + safeAffixes.size() + " maximum=" + MAX_ITEM_AFFIXES);
        }

        int expandedRules = 0;

        for (EntryDefinition entry : safeEntries) {
            Optional<String> problem = findEntryProblem(entry);

            if (problem.isPresent()) {
                return problem;
            }

            expandedRules += entry.rules().size();

            if (expandedRules > MAX_EXPANDED_ITEM_RULES) {
                return Optional.of(
                        "expanded_rule_count="
                                + expandedRules
                                + " maximum="
                                + MAX_EXPANDED_ITEM_RULES);
            }
        }

        for (AffixDefinition affix : safeAffixes) {
            if (affix == null) {
                return Optional.of("null_affix");
            }

            if (affix.entries().size() > MAX_AFFIX_ENTRIES) {
                return Optional.of(
                        "affix="
                                + affix.id()
                                + " entry_count="
                                + affix.entries().size()
                                + " maximum="
                                + MAX_AFFIX_ENTRIES);
            }

            Optional<String> displayProblem =
                    findDisplayProblem(
                            affix.display().name(),
                            affix.display().authoredSummary(),
                            affix.display().flavorText());

            if (displayProblem.isPresent()) {
                return Optional.of("affix=" + affix.id() + " " + displayProblem.get());
            }

            for (EntryDefinition entry : affix.entries()) {
                Optional<String> problem = findEntryProblem(entry);

                if (problem.isPresent()) {
                    return problem;
                }

                expandedRules += entry.rules().size();

                if (expandedRules > MAX_EXPANDED_ITEM_RULES) {
                    return Optional.of(
                            "expanded_rule_count="
                                    + expandedRules
                                    + " maximum="
                                    + MAX_EXPANDED_ITEM_RULES);
                }
            }
        }

        return findAggregateBudgetProblem(safeEntries, safeAffixes);
    }

    public static DataResult<List<EntryDefinition>> validateEntryComponentCodec(
            List<EntryDefinition> entries) {
        Optional<String> problem = findEntryComponentProblem(entries);

        return problem.<DataResult<List<EntryDefinition>>>map(
                        reason ->
                                DataResult.error(
                                        () -> "Invalid DamageModule entry component: " + reason))
                .orElseGet(() -> DataResult.success(List.copyOf(entries)));
    }

    public static DataResult<List<AffixDefinition>> validateAffixComponentCodec(
            List<AffixDefinition> affixes) {
        Optional<String> problem = findAffixComponentProblem(affixes);

        return problem.<DataResult<List<AffixDefinition>>>map(
                        reason ->
                                DataResult.error(
                                        () -> "Invalid DamageModule affix component: " + reason))
                .orElseGet(() -> DataResult.success(List.copyOf(affixes)));
    }

    public static Optional<String> findEntryComponentProblem(List<EntryDefinition> entries) {
        return findComponentProblem(entries == null ? List.of() : entries, List.of());
    }

    public static Optional<String> findAffixComponentProblem(List<AffixDefinition> affixes) {
        return findComponentProblem(List.of(), affixes == null ? List.of() : affixes);
    }

    public static Optional<RuleCost> measureRuleCost(DamageRuleDefinition rule) {
        if (rule == null || rule.conditions() == null || rule.operations() == null) {
            return Optional.empty();
        }

        if (findRuleProblem(rule).isPresent()) {
            return Optional.empty();
        }

        Optional<Integer> conditionNodes = countConditionNodes(rule.conditions());
        return conditionNodes.map(nodes -> new RuleCost(nodes, rule.operations().size()));
    }

    public static Optional<String> findEntryProblem(EntryDefinition entry) {
        if (entry == null) {
            return Optional.of("null_entry");
        }

        if (entry.rules().size() > MAX_ENTRY_RULES) {
            return Optional.of(
                    "entry="
                            + entry.id()
                            + " rule_count="
                            + entry.rules().size()
                            + " maximum="
                            + MAX_ENTRY_RULES);
        }

        Optional<String> displayProblem =
                findDisplayProblem(
                        entry.display().name(),
                        entry.display().authoredSummary(),
                        entry.display().flavorText());

        if (displayProblem.isPresent()) {
            return Optional.of("entry=" + entry.id() + " " + displayProblem.get());
        }

        for (DamageRuleDefinition rule : entry.rules()) {
            Optional<String> ruleProblem = findRuleProblem(rule);

            if (ruleProblem.isPresent()) {
                return Optional.of(
                        "entry="
                                + entry.id()
                                + " rule="
                                + safeRuleId(rule)
                                + " "
                                + ruleProblem.get());
            }
        }

        return Optional.empty();
    }

    public static Optional<String> findDisplayTextProblem(DisplayText text) {
        if (text == null) {
            return Optional.of("display_text_is_null");
        }

        return DisplayText.validationProblem(text);
    }

    private static Optional<String> findDisplayProblem(
            Optional<DisplayText> name, List<DisplayText> tooltip, Optional<DisplayText> flavor) {
        if (tooltip == null) {
            return Optional.of("tooltip_is_null");
        }

        if (tooltip.size() > MAX_TOOLTIP_LINES) {
            return Optional.of(
                    "tooltip_line_count=" + tooltip.size() + " maximum=" + MAX_TOOLTIP_LINES);
        }

        if (name == null) {
            return Optional.of("display_name_optional_is_null");
        }
        if (name.isPresent()) {
            Optional<String> nameProblem = findDisplayTextProblem(name.get());
            if (nameProblem.isPresent()) {
                return nameProblem;
            }
        }

        for (DisplayText line : tooltip) {
            Optional<String> lineProblem = findDisplayTextProblem(line);

            if (lineProblem.isPresent()) {
                return lineProblem;
            }
        }

        if (flavor != null && flavor.isPresent()) {
            return findDisplayTextProblem(flavor.get());
        }

        return Optional.empty();
    }

    private static Optional<String> findComponentProblem(
            List<EntryDefinition> entries, List<AffixDefinition> affixes) {
        return findItemProblem(entries, affixes);
    }

    private static Optional<String> findAggregateBudgetProblem(
            List<EntryDefinition> entries, List<AffixDefinition> affixes) {
        ComponentBudget budget = new ComponentBudget();

        for (EntryDefinition entry : entries) {
            Optional<String> problem = budget.addEntry(entry);

            if (problem.isPresent()) {
                return problem;
            }
        }

        for (AffixDefinition affix : affixes) {
            Optional<String> displayProblem = budget.addDisplay(affix.display());

            if (displayProblem.isPresent()) {
                return displayProblem;
            }

            for (EntryDefinition entry : affix.entries()) {
                Optional<String> problem = budget.addEntry(entry);

                if (problem.isPresent()) {
                    return problem;
                }
            }
        }

        return Optional.empty();
    }

    static long displayCodePoints(DisplayText text) {
        long total = 0L;

        if (text == null) {
            return total;
        }

        {
            java.util.Objects.requireNonNull(text);
            if (text instanceof DisplayText.Literal literal) {
                total += codePointLength(literal.text());
            } else if (text instanceof DisplayText.Translatable translatable) {
                total += codePointLength(translatable.key());
                if (translatable.fallback().isPresent()) {
                    total += codePointLength(translatable.fallback().get());
                }
                for (var argument : translatable.args()) {
                    total += argument.codePoints();
                }
            } else {
                throw new IllegalStateException("Unexpected sealed subtype");
            }
        }

        return total;
    }

    static long displayCodePoints(
            Optional<DisplayText> name, List<DisplayText> tooltip, Optional<DisplayText> flavor) {
        long total = name.map(DamageRuleLimits::displayCodePoints).orElse(0L);

        for (DisplayText line : tooltip) {
            total += displayCodePoints(line);
        }

        if (flavor.isPresent()) {
            total += displayCodePoints(flavor.get());
        }

        return total;
    }

    private static int codePointLength(String value) {
        return value.codePointCount(0, value.length());
    }

    private static String safeRuleId(DamageRuleDefinition rule) {
        return rule == null || rule.id() == null ? "<null>" : rule.id().toString();
    }

    static <T> Optional<String> rawProblemForTesting(
            DynamicOps<T> ops,
            T input,
            int maximumDepth,
            int maximumNodes,
            RawTraversalObserver observer) {
        return rawProblem(ops, input, maximumDepth, maximumNodes, observer);
    }

    private static <T> Optional<String> rawProblem(
            DynamicOps<T> ops,
            T input,
            int maximumDepth,
            int maximumNodes,
            RawTraversalObserver observer) {
        return CodecBudgets.rawProblem(ops, input, maximumDepth, maximumNodes, observer);
    }

    public record RuleCost(int conditionNodes, int operations) {}

    @FunctionalInterface
    interface RawTraversalObserver extends CodecBudgets.RawTraversalObserver {}
}
