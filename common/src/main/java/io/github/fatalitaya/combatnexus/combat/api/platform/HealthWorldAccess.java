package io.github.fatalitaya.combatnexus.combat.api.platform;

import io.github.fatalitaya.combatnexus.combat.health.api.LethalAttempt;
import java.util.Optional;
import java.util.UUID;

/** World-owned access after command admission validates the target. */
public interface HealthWorldAccess {
    HealthTarget target(UUID entity);
    /** Empty means the requested damage type is not registered. */
    Optional<LethalTarget> lethal(LethalAttempt command);
}
