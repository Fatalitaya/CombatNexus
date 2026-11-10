package io.github.fatalitaya.combatnexus.combat.damage.api.display.document;

import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryStacking;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;
import java.util.Optional;

public record EntryTooltipView(
        NexusId id,
        Optional<DisplayText> name,
        List<DisplayText> authoredSummary,
        Optional<DisplayText> flavorText,
        EntryStacking stacking,
        Optional<NexusId> stackingGroup,
        RuleBreakdownPolicy breakdownPolicy,
        List<RuleTooltipView> rules
) implements TooltipSection {
    public EntryTooltipView {
        name = name == null ? Optional.empty() : name;
        authoredSummary = authoredSummary == null
                ? List.of() : List.copyOf(authoredSummary);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
        stackingGroup = stackingGroup == null ? Optional.empty() : stackingGroup;
        rules = rules == null ? List.of() : List.copyOf(rules);
    }
}
