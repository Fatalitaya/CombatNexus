package io.github.fatalitaya.combatnexus.combat.damage.api.display;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;

/** Compact frozen numeric/source facts. No maps, live context or formatted tooltip lines. */
public record ContributionFact(long generation, int operationIndex, NexusId id,
        DamageContributionSourceKind sourceKind, DamageContributionOperationKind operationKind,
        DamagePhase phase, Optional<NexusId> channel, Optional<DamageApplicationBucket> applicationBucket,
        Optional<NexusId> preMultiplierBucket, float value, DamageContributionStatus status,
        Optional<NexusId> displayGroup, Optional<NexusId> displaySubgroup, RuleDetails details) implements ContributionRecord {
    public record RuleDetails(NexusId rule, NexusId operation, DamageRuleOwnerKind ownerKind,
            Optional<NexusId> affix, Optional<NexusId> entry, DamageRuleProviderType provider,
            RuleSourceLocation location, DamageRuleRole role, DamageMutationResult result, Optional<NexusId> installationSource) {}
    public ContributionFact {
        Objects.requireNonNull(id); Objects.requireNonNull(details); Objects.requireNonNull(status);
        if (generation < 1 || operationIndex < 0)
            throw new IllegalArgumentException("Invalid contribution fact");
    }
    public ContributionFact withStatus(DamageContributionStatus status) {
        return new ContributionFact(generation, operationIndex, id, sourceKind, operationKind, phase, channel,
                applicationBucket, preMultiplierBucket, value, status, displayGroup, displaySubgroup, details);
    }
}
