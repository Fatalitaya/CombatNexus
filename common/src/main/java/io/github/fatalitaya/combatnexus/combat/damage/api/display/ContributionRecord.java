package io.github.fatalitaya.combatnexus.combat.damage.api.display;
/** Closed, immutable explanation inputs; text is assembled only by the projector. */
public sealed interface ContributionRecord permits ContributionFact, VanillaContributionFact, DefensePenetrationFact {
    DamageContributionStatus status();
    ContributionRecord withStatus(DamageContributionStatus status);
}
