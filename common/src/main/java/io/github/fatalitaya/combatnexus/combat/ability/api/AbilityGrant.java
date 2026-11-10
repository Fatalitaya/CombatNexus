package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceInstance;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import java.util.*;

/** Detached grant identity. This value is evidence, never application authority. */
public record AbilityGrant(UUID id, UUID world, ParticipantIdentity actor, SourceInstance source,
                           NexusId contribution, NexusId ability, long generation) {
    public AbilityGrant {
        Objects.requireNonNull(id); Objects.requireNonNull(world); Objects.requireNonNull(actor);
        Objects.requireNonNull(source); Objects.requireNonNull(contribution); Objects.requireNonNull(ability);
        if (generation < 1) throw new IllegalArgumentException("Invalid ability generation");
    }
}
