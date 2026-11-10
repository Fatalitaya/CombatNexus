package io.github.fatalitaya.combatnexus.combat.api.source;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** A provider's concrete slot or cast, distinct from a definition and from installation authority. */
public record SourceInstance(UUID instance, NexusId provider, Optional<UUID> owner, String slot) {
    public SourceInstance {
        Objects.requireNonNull(instance); Objects.requireNonNull(provider); Objects.requireNonNull(owner); Objects.requireNonNull(slot);
        if (slot.length() > 512) throw new IllegalArgumentException("Source slot too long");
    }
}
