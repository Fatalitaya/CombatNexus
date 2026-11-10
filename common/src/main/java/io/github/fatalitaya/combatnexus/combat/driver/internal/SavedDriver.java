package io.github.fatalitaya.combatnexus.combat.driver.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope;
import java.util.*;

/** Ownership only. Restored claims remain unbound until a genuine credential reconnects. */
public record SavedDriver(NexusId driver, Set<DriverScope> scopes, NexusId source) {
    public SavedDriver { Objects.requireNonNull(driver); scopes = Set.copyOf(scopes); if (source != null && scopes.contains(DriverScope.NUMERICAL)) throw new IllegalArgumentException("Numerical ownership requires an entity range"); if (scopes.isEmpty()) throw new IllegalArgumentException("Empty saved driver scopes"); }
}
