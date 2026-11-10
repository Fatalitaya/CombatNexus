package io.github.fatalitaya.combatnexus.combat.api.scheduling;
import java.util.UUID;
import io.github.fatalitaya.combatnexus.combat.api.*;
/** Expires after one invocation. The executor appends every receipt, including refused commands. */
public interface IntentExecution {
    UUID target();
    long gameTick();
    CombatSource source();
    CommandOutcome<?> submit(DomainCommand command);
    CommandOutcome<?> damage(double amount, io.github.fatalitaya.combatnexus.api.id.NexusId type);
}
