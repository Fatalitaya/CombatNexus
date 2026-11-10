package io.github.fatalitaya.combatnexus.combat.api.behavior;

import io.github.fatalitaya.combatnexus.combat.api.CommandOutcome;
import io.github.fatalitaya.combatnexus.combat.api.DomainCommand;

/** Short-lived execution capability; every submitted command is included in the parent effect receipt. */
public interface InstantExecution {
    BehaviorSource source();
    CommandOutcome<?> submit(DomainCommand command);
    CommandOutcome<?> damage(double amount, io.github.fatalitaya.combatnexus.api.id.NexusId type, java.util.Optional<java.util.UUID> direct);
}
