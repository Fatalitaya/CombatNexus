package io.github.fatalitaya.combatnexus.combat.api.transaction;

import java.util.Objects;
import java.util.UUID;

/** One entity instance during this process, including instances not yet added to a world. */
public record ParticipantIdentity(UUID entity, long incarnation) {
    public ParticipantIdentity {
        Objects.requireNonNull(entity);
        if (incarnation <= 0) throw new IllegalArgumentException("Invalid participant incarnation");
    }
}
