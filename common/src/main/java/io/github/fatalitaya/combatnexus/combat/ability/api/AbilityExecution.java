package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatAction;
import java.util.UUID;

/** Callback-scoped domain command capability. Retaining it does not retain permission. */
public interface AbilityExecution {
    enum Invocation { FIRST, AUTOMATIC, INPUT }
    Invocation invocation();
    void released();
    AbilityInput input();
    CombatAction action();
    int step();
    AbilityParameters state();
    CombatSource source();
    long businessTick();
    long businessTick(UUID target, NexusId source);
    CommandOutcome<?> submit(DomainCommand command);
    CommandOutcome<?> damage(UUID target, double amount, NexusId damageType);
}
