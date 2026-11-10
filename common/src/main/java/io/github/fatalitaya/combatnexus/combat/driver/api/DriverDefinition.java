package io.github.fatalitaya.combatnexus.combat.driver.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;
import java.util.Set;

/** Startup declaration. EXTERNAL means the resolved amount already accounts for that mechanism.
 * managedProtection covers CombatNexus protection behaviors; native death-protection events remain native. */
public record DriverDefinition(NexusId id, Set<DriverScope> scopes, ApplicationOwner shields,
        ApplicationOwner absorption, ApplicationOwner managedProtection) {
    public enum ApplicationOwner { COMBATNEXUS, EXTERNAL }
    public DriverDefinition {
        Objects.requireNonNull(id); scopes = Set.copyOf(scopes);
        Objects.requireNonNull(shields); Objects.requireNonNull(absorption); Objects.requireNonNull(managedProtection);
        if (scopes.isEmpty()) throw new IllegalArgumentException("Driver must declare a scope");
    }
}
