package io.github.fatalitaya.combatnexus.combat.api.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Optional;
import java.util.Set;

/** World registry observations used to validate shield eligibility. */
public interface DamageTypeCatalog {
    boolean contains(NexusId type);
    Optional<Set<NexusId>> tag(NexusId tag);
}
