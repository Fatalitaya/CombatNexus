package io.github.fatalitaya.combatnexus.combat.damage.api.display.narrative;

import io.github.fatalitaya.combatnexus.presentation.api.phrase.RulePhrase;

import java.util.List;
import java.util.Objects;

public record RuleNarrative(
        ConditionExpression condition,
        List<RulePhrase> effects
) {
    public RuleNarrative {
        Objects.requireNonNull(condition, "condition");
        effects = effects == null ? List.of() : List.copyOf(effects);
    }
}
