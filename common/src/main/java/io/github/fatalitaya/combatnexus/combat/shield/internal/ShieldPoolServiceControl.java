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
    /** Preview without retaining a reservation or consuming its application identity. */
    double previewRemaining(UUID target, long damageId, io.github.fatalitaya.combatnexus.api.id.NexusId type,
            double amount, Map<io.github.fatalitaya.combatnexus.api.id.NexusId, Float> channels);
    Reservation reserveAfterReaction(UUID target, long damageId, io.github.fatalitaya.combatnexus.api.id.NexusId type,
            double amount, Map<io.github.fatalitaya.combatnexus.api.id.NexusId, Float> channels,
            double maximumAbsorption, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> existingSources);
    void close();
    void tick(long gameTick);
    void clear(UUID target);
    void reset();
    List<Change> drainChanges();
}
