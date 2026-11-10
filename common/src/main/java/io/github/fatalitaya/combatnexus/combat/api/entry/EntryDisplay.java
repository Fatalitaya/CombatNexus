package io.github.fatalitaya.combatnexus.combat.api.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleLimits;

import java.util.List;
import java.util.Optional;

public record EntryDisplay(
        Optional<DisplayText> name,
        List<DisplayText> authoredSummary,
        Optional<DisplayText> flavorText,
        boolean showRuleBreakdown
) {
    public static final EntryDisplay EMPTY =
            new EntryDisplay(
                    Optional.empty(),
                    List.of(),
                    Optional.empty(),
                    false
            );

    public static final Codec<EntryDisplay> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DisplayText.CODEC.optionalFieldOf("name").forGetter(EntryDisplay::name),
                    DamageRuleLimits.boundedList(DisplayText.CODEC, DamageRuleLimits.MAX_TOOLTIP_LINES,
                                    "entry authored summary")
                            .optionalFieldOf("authored_summary", List.of()).forGetter(EntryDisplay::authoredSummary),
                    DisplayText.CODEC.optionalFieldOf("flavor_text").forGetter(EntryDisplay::flavorText),
                    Codec.BOOL.optionalFieldOf("show_rule_breakdown", false).forGetter(EntryDisplay::showRuleBreakdown)
            ).apply(instance, EntryDisplay::new));

    public EntryDisplay {
        name = name == null ? Optional.empty() : name;
        authoredSummary = authoredSummary == null ? List.of() : List.copyOf(authoredSummary);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
    }

    public boolean hasVisibleText() {
        return name.isPresent()
                || !authoredSummary.isEmpty()
                || flavorText.isPresent()
                || showRuleBreakdown;
    }

    public EntryDisplay(
            DisplayText name,
            List<DisplayText> authoredSummary,
            Optional<DisplayText> flavorText,
            boolean showRuleBreakdown
    ) {
        this(Optional.ofNullable(name), authoredSummary, flavorText, showRuleBreakdown);
    }
}
