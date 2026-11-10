package io.github.fatalitaya.combatnexus.combat.damage.api.display;

import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;
import java.util.Optional;

public record DamageContributionDescriptor(
        NexusId id,
        DamageContributionSourceKind sourceKind,
        DamageContributionOperationKind operationKind,
        DamagePhase phase,

        Optional<NexusId> channel,
        Optional<DamageApplicationBucket> applicationBucket,
        Optional<NexusId> preMultiplierBucket,

        float value,

        DamageContributionStatus status,

        Optional<NexusId> displayGroup,
        Optional<NexusId> displaySubgroup,
        Optional<String> displayName,
        List<String> tooltipLines,
        boolean runtimeOnly
) {
    public DamageContributionDescriptor {
        if (id == null) {
            throw new IllegalArgumentException("Damage contribution id cannot be null");
        }

        if (sourceKind == null) {
            sourceKind = DamageContributionSourceKind.UNKNOWN;
        }

        if (operationKind == null) {
            throw new IllegalArgumentException("Damage contribution operation kind cannot be null: " + id);
        }

        if (phase == null) {
            throw new IllegalArgumentException("Damage contribution phase cannot be null: " + id);
        }

        if (status == null) {
            status = DamageContributionStatus.APPLIED;
        }

        channel = channel == null ? Optional.empty() : channel;
        applicationBucket = applicationBucket == null ? Optional.empty() : applicationBucket;
        preMultiplierBucket = preMultiplierBucket == null ? Optional.empty() : preMultiplierBucket;
        displayGroup = displayGroup == null ? Optional.empty() : displayGroup;
        displaySubgroup = displaySubgroup == null ? Optional.empty() : displaySubgroup;
        displayName = displayName == null ? Optional.empty() : displayName;
        tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
    }

    public DamageContributionDescriptor withStatus(
            DamageContributionStatus status
    ) {
        return new DamageContributionDescriptor(
                id,
                sourceKind,
                operationKind,
                phase,
                channel,
                applicationBucket,
                preMultiplierBucket,
                value,
                status,
                displayGroup,
                displaySubgroup,
                displayName,
                tooltipLines,
                runtimeOnly);
    }
}
