package io.github.fatalitaya.combatnexus.combat.healing.api;

import java.util.Set;
import java.util.function.Consumer;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;

/** Valid only during the startup registration event. All handlers receive frozen participant facts. */
public interface HealingRegistrar {
    void processor(NexusId id, HealingPhase phase, int priority, Consumer<HealingCalculation> processor);
    void observe(NexusId id, int priority, Consumer<HealingSettlement> observer);
    void react(NexusId id, int priority, Set<CombatOperation> permissions, Consumer<CombatCallback<HealingSettlement>> reaction);
}
