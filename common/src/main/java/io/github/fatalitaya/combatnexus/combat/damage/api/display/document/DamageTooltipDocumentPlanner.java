package io.github.fatalitaya.combatnexus.combat.damage.api.display.document;

import io.github.fatalitaya.combatnexus.combat.damage.api.rule.CompositeDamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOrdering;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.narrative.RuleNarrativePlanner;

import java.util.List;
import java.util.Objects;

public final class DamageTooltipDocumentPlanner {
    private final RuleNarrativePlanner narratives;

    public DamageTooltipDocumentPlanner(RuleNarrativePlanner narratives) {
        this.narratives = Objects.requireNonNull(narratives, "narratives");
    }

    public DamageTooltipDocument plan(
            List<EntryDefinition> standaloneEntries,
            List<AffixDefinition> affixes,
            List<VanillaTooltipAugmentation> vanillaAugmentations
    ) {
        List<EntryTooltipView> entryViews = safe(standaloneEntries).stream()
                .filter(Objects::nonNull)
                .map(this::entry)
                .toList();
        List<AffixTooltipView> affixViews = safe(affixes).stream()
                .filter(Objects::nonNull)
                .map(this::affix)
                .toList();
        return new DamageTooltipDocument(
                affixViews,
                entryViews,
                safe(vanillaAugmentations));
    }

    private AffixTooltipView affix(AffixDefinition affix) {
        return new AffixTooltipView(
                affix.id(),
                affix.display().name(),
                affix.display().authoredSummary(),
                affix.display().flavorText(),
                affix.rarity(),
                affix.stacking(),
                affix.stackingGroup(),
                RuleBreakdownPolicy.fromDisplay(
                        affix.display().showRuleBreakdown(),
                        !affix.display().authoredSummary().isEmpty()
                ),
                affix.entries().stream()
                                .map(this::entry)
                        .toList()
        );
    }

    private EntryTooltipView entry(EntryDefinition entry) {
        return new EntryTooltipView(
                entry.id(),
                entry.display().name(),
                entry.display().authoredSummary(),
                entry.display().flavorText(),
                entry.stacking(),
                entry.stackingGroup(),
                RuleBreakdownPolicy.fromDisplay(
                        entry.display().showRuleBreakdown(),
                        !entry.display().authoredSummary().isEmpty()
                ),
                sortedRules(entry).stream()
                        .map(this::rule)
                        .toList()
        );
    }

    private static int compareEntryExecutionOrder(
            EntryDefinition first,
            EntryDefinition second
    ) {
        int ruleComparison = DamageRuleOrdering.compareDefinitionSequences(
                sortedRules(first),
                sortedRules(second)
        );
        return ruleComparison != 0
                ? ruleComparison
                : first.id().compareNamespaced(second.id());
    }

    /**
     * Affix blocks deliberately remain contiguous. A multi-phase affix cannot
     * be both globally interleaved rule-by-rule and kept as one visual block,
     * so its contained execution-ordered rule sequence determines the block's
     * placement.
     */
    private static int compareAffixExecutionOrder(
            AffixDefinition first,
            AffixDefinition second
    ) {
        int ruleComparison = DamageRuleOrdering.compareDefinitionSequences(
                sortedRules(first),
                sortedRules(second)
        );
        return ruleComparison != 0
                ? ruleComparison
                : first.id().compareNamespaced(second.id());
    }

    private static List<DamageRuleDefinition> sortedRules(
            EntryDefinition entry
    ) {
        return DamageRuleOrdering.sortedDefinitions(entry.rules());
    }

    private static List<DamageRuleDefinition> sortedRules(
            AffixDefinition affix
    ) {
        return affix.entries().stream()
                .flatMap(entry -> entry.rules().stream())
                .sorted(DamageRuleOrdering.DEFINITION_EXECUTION_ORDER)
                .toList();
    }

    private RuleTooltipView rule(DamageRuleDefinition rule) {
        return new RuleTooltipView(
                rule.id(),
                narratives.plan(rule),
                rule.phase(),
                rule.role(),
                rule.priority(),
                rule.stacking(),
                rule.stackingGroup(),
                conditionTypes(rule.conditions()),
                rule.operations().stream().map(operation -> operation.type()).toList()
        );
    }

    private static List<io.github.fatalitaya.combatnexus.api.id.NexusId> conditionTypes(
            List<DamageRuleCondition> conditions
    ) {
        java.util.ArrayList<io.github.fatalitaya.combatnexus.api.id.NexusId> types =
                new java.util.ArrayList<>();
        for (DamageRuleCondition condition : safe(conditions)) {
            collectConditionTypes(condition, types);
        }
        return List.copyOf(types);
    }

    private static void collectConditionTypes(
            DamageRuleCondition condition,
            List<io.github.fatalitaya.combatnexus.api.id.NexusId> types
    ) {
        if (condition == null) {
            return;
        }
        types.add(condition.type());
        if (condition instanceof CompositeDamageRuleCondition composite) {
            for (DamageRuleCondition child : composite.childConditions()) {
                collectConditionTypes(child, types);
            }
        }
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
