package io.github.fatalitaya.combatnexus.combat.health.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CombatSource;

/** A lethal intent, never ordinary damage. Native protection honors the damage type's bypass tags. */
public record LethalAttempt(UUID requestId, UUID target, CombatSource source, NexusId damageType,
        Optional<UUID> attacker, boolean bypassManagedProtection) {
    public LethalAttempt {
        Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source);
        Objects.requireNonNull(damageType); Objects.requireNonNull(attacker);
    }
}
