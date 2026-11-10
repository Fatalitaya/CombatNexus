package io.github.fatalitaya.combatnexus.attribute.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Immutable source blocks from the same capture as the value frame. */
public record AttributeProvenance(UUID entity, long gameTick, long revision,
        Map<NexusId, List<AttributeSnapshot.Contribution>> sources) {
    public AttributeProvenance {
        Objects.requireNonNull(entity);
        var copy = new LinkedHashMap<NexusId, List<AttributeSnapshot.Contribution>>();
        sources.forEach((id, block) -> copy.put(id, List.copyOf(block)));
        sources = Map.copyOf(copy);
    }
}
