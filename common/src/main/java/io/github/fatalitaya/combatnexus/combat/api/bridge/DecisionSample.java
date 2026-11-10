package io.github.fatalitaya.combatnexus.combat.api.bridge;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Detached result of a shared action decision; carries no random generator or execution authority. */
public record DecisionSample(NexusId policy, NexusId decision, Optional<UUID> target, float value) {
    public DecisionSample {
        Objects.requireNonNull(policy); Objects.requireNonNull(decision); Objects.requireNonNull(target);
        if (!Float.isFinite(value) || value < 0 || value >= 1) throw new IllegalArgumentException("Invalid decision sample");
    }
}
