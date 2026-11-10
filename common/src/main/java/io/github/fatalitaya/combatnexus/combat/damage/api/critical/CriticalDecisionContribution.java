package io.github.fatalitaya.combatnexus.combat.damage.api.critical;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;

/** An immutable, registry-authored decision contribution. */
public record CriticalDecisionContribution(
        NexusId sourceId,
        int priority,
        CriticalDecision decision
) {
    public CriticalDecisionContribution {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(decision, "decision");
        if (decision == CriticalDecision.DEFAULT) {
            throw new IllegalArgumentException("DEFAULT is not a contribution");
        }
    }
}
