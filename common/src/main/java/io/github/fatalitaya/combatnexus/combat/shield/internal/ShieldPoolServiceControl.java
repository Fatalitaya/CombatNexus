package io.github.fatalitaya.combatnexus.combat.shield.internal;
import io.github.fatalitaya.combatnexus.combat.shield.spi.ShieldPoolService;
import io.github.fatalitaya.combatnexus.combat.shield.api.*;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingService;

/** Composition-root lifecycle contract; not a Java security sandbox. */
public interface ShieldPoolServiceControl extends ShieldPoolService {
    List<SavedShield> save(UUID target, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> sources);
    void restore(UUID target, List<SavedShield> saved, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> sources, Map<io.github.fatalitaya.combatnexus.api.id.NexusId, io.github.fatalitaya.combatnexus.api.id.NexusId> pools);

    void suspend(UUID target,io.github.fatalitaya.combatnexus.api.id.NexusId source,boolean suspended);
    void refresh(UUID target,io.github.fatalitaya.combatnexus.api.id.NexusId source,ShieldDefinition definition);
    void close();
    void tick(long gameTick);
    void clear(UUID target);
    void reset();
    List<Change> drainChanges();
}
