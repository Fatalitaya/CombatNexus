package io.github.fatalitaya.combatnexus.combat.damage.api.display;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.*;
/** Frozen native bridge inputs, without rendered names or detail lines. */
public record VanillaContributionFact(Kind kind, NexusId id, DamageContributionSourceKind sourceKind,
        DamagePhase phase, NexusId channel, DamageApplicationBucket bucket, NexusId preMultiplierBucket,
        float value, String sourceLabel, DamageContributionStatus status) implements ContributionRecord {
    public enum Kind { BASE, MULTIPLIER, RESISTANCE, MITIGATION, ARMOR }
    public VanillaContributionFact withStatus(DamageContributionStatus status) {
        return new VanillaContributionFact(kind, id, sourceKind, phase, channel, bucket, preMultiplierBucket, value, sourceLabel, status);
    }
}
