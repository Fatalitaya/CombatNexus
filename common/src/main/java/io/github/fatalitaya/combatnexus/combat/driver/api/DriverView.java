package io.github.fatalitaya.combatnexus.combat.driver.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import java.util.Optional;
import java.util.Set;

public record DriverView(ParticipantIdentity target, NexusId driver, Set<DriverScope> scopes,
        Optional<NexusId> source, boolean bound) {
    public DriverView { scopes = Set.copyOf(scopes); }
}
