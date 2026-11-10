package io.github.fatalitaya.combatnexus.combat.damage.api.display.document;

import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixRarity;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixStacking;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;
import java.util.Optional;

public record AffixTooltipView(
        NexusId id,
        Optional<DisplayText> name,
        List<DisplayText> authoredSummary,
        Optional<DisplayText> flavorText,
        AffixRarity rarity,
        AffixStacking stacking,
        Optional<NexusId> stackingGroup,
        RuleBreakdownPolicy breakdownPolicy,
        List<EntryTooltipView> entries
) implements TooltipSection {
    public AffixTooltipView {
        name = name == null ? Optional.empty() : name;
        authoredSummary = authoredSummary == null
                ? List.of() : List.copyOf(authoredSummary);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
        stackingGroup = stackingGroup == null ? Optional.empty() : stackingGroup;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
