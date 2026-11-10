package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Optional;
import java.util.Set;

/** Empty identity denotes a direct, unregistered holder; tags remain immutable observations. */
public record DamageTypeFacts(Optional<NexusId> id, Set<NexusId> tags) {
    public DamageTypeFacts { java.util.Objects.requireNonNull(id); tags = Set.copyOf(tags); }
}
