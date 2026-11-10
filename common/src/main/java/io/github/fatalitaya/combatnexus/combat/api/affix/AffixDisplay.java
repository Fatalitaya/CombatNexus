package io.github.fatalitaya.combatnexus.combat.api.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleLimits;

import java.util.List;
import java.util.Optional;

public record AffixDisplay(
        Optional<DisplayText> name,
        List<DisplayText> authoredSummary,
        Optional<DisplayText> flavorText,
        boolean showRuleBreakdown
) {
    public static final AffixDisplay EMPTY =
            new AffixDisplay(
                    Optional.empty(),
                    List.of(),
                    Optional.empty(),
                    false
            );

    public static final Codec<AffixDisplay> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DisplayText.CODEC.optionalFieldOf("name").forGetter(AffixDisplay::name),
                    DamageRuleLimits.boundedList(DisplayText.CODEC, DamageRuleLimits.MAX_TOOLTIP_LINES,
                                    "affix authored summary")
                            .optionalFieldOf("authored_summary", List.of()).forGetter(AffixDisplay::authoredSummary),
                    DisplayText.CODEC.optionalFieldOf("flavor_text").forGetter(AffixDisplay::flavorText),
                    Codec.BOOL.optionalFieldOf("show_rule_breakdown", false).forGetter(AffixDisplay::showRuleBreakdown)
            ).apply(instance, AffixDisplay::new));

    public AffixDisplay {
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

    public AffixDisplay(
            DisplayText name,
            List<DisplayText> authoredSummary,
            Optional<DisplayText> flavorText,
            boolean showRuleBreakdown
    ) {
        this(Optional.ofNullable(name), authoredSummary, flavorText, showRuleBreakdown);
    }
}
