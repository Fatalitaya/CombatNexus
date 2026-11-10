package io.github.fatalitaya.combatnexus.combat.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Stable contribution identity. Unknown external owners remain absent, never inferred. */
public record CombatSource(NexusId id, Optional<UUID> owner, Optional<CombatOperationRef> parentOperation) {
    public CombatSource {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(parentOperation, "parentOperation");
    }
}
