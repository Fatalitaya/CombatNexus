package io.github.fatalitaya.combatnexus.combat.damage.api.display;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput;

/** Frozen input transformation; neither channel resistance nor final damage reduction. */
public record DefensePenetrationFact(NexusId source, DefenseInput input, float ratio, float flat,
        float before, float after, DamageContributionStatus status) implements ContributionRecord {
    @Override public DefensePenetrationFact withStatus(DamageContributionStatus status) {
        return new DefensePenetrationFact(source, input, ratio, flat, before, after, status);
    }
}
