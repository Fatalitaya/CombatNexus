package io.github.fatalitaya.combatnexus.attribute.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.List;
import java.util.UUID;

/** Values and provenance read in one platform capture, before publication of a revision. */
public record AttributeObservation(UUID entity, long gameTick, List<Entry> entries) {
    public AttributeObservation { java.util.Objects.requireNonNull(entity); entries = List.copyOf(entries); }
    public record Entry(NexusId attribute, AttributeValueFrame.Value value, List<AttributeSnapshot.Contribution> contributions) {
        public Entry { java.util.Objects.requireNonNull(attribute); java.util.Objects.requireNonNull(value); contributions = List.copyOf(contributions); }
    }
}
