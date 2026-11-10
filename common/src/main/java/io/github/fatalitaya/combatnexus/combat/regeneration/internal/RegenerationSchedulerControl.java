package io.github.fatalitaya.combatnexus.combat.regeneration.internal;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.*;
import java.util.*;

/** Composition-root lifecycle contract; not a Java security sandbox. */
public interface RegenerationSchedulerControl extends RegenerationScheduler {
    List<SavedRecovery> save(UUID target, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> sources, long now);
    void restore(UUID target, List<SavedRecovery> saved, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> sources);
    List<RegenerationPlan> snapshot(UUID target);

    void recordDamage(UUID target, long gameTick);
    void tick(long gameTick, java.util.function.Consumer<io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest> healing);
    void clear(UUID target);
    void reset();
    void suspend(java.util.UUID target,io.github.fatalitaya.combatnexus.api.id.NexusId source,boolean suspended);
    void refresh(java.util.UUID target,io.github.fatalitaya.combatnexus.api.id.NexusId source,io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationDefinition definition);
    void close();
}
