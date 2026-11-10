package io.github.fatalitaya.combatnexus.combat.api;
import java.util.*;
import java.util.function.IntConsumer;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceInstance;
/** Read-only lookup of an already loaded world; does not create or advance a session. */
public interface WorldLookup {
    Optional<CombatWorld> combatWorld();
    List<EffectInstance> managedEffects(UUID target, IntConsumer budget);
    Optional<SourceInstance> source(UUID target, NexusId source);
    int restrictionMask(UUID target);
    Optional<UUID> currentSession();
}
