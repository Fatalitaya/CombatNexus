package io.github.fatalitaya.combatnexus.combat.api.platform;

import java.util.Optional;
import java.util.UUID;

/** Bound to one loaded world; a missing or nonliving entity has no absorption target. */
@FunctionalInterface
public interface AbsorptionWorldAccess {
    Optional<AbsorptionTarget> find(UUID entity);
}
