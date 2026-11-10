package io.github.fatalitaya.combatnexus.api.entity;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;
import java.util.Set;

/** Registry identity and tag membership frozen before rule callbacks run. */
public record EntityTypeFacts(NexusId id, Set<NexusId> tags) {
    public EntityTypeFacts { Objects.requireNonNull(id); tags = Set.copyOf(tags); }
}
