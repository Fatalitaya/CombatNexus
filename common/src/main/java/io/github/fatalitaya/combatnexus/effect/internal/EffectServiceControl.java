package io.github.fatalitaya.combatnexus.effect.internal;
import io.github.fatalitaya.combatnexus.effect.api.*;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingService;

/** Composition-root lifecycle contract; not a Java security sandbox. */
public interface EffectServiceControl extends EffectService {
    void restoring(boolean value);
    void suspend(UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId source, boolean suspended);
    boolean suspended(UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId source);
    Set<io.github.fatalitaya.combatnexus.api.id.NexusId> persistentSources(UUID target);
    List<EffectInstance> save(UUID target, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> sources);
    void restore(UUID target, List<EffectInstance> saved, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> sources);
    void close();
    void tick(long gameTick);
    void clear(UUID target, Removal reason);
    void reset();
    List<Change> drainChanges();
}
