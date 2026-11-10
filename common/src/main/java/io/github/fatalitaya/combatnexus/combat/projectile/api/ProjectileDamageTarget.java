package io.github.fatalitaya.combatnexus.combat.projectile.api;

import java.util.Optional;
import java.util.UUID;

/** Exact arrow binding for a short-lived launch transaction. */
public interface ProjectileDamageTarget {
    record LaunchIdentity(UUID action, UUID shooter, long gameTick) {
        public LaunchIdentity { java.util.Objects.requireNonNull(action); java.util.Objects.requireNonNull(shooter); }
    }
    Optional<LaunchIdentity> launch();
    double damage();
    void damage(double value);
}
