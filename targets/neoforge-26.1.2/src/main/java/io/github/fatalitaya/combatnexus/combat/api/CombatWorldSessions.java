package io.github.fatalitaya.combatnexus.combat.api;

import java.util.*;


/** Read-only session lookup for domain ports. Session IDs are not mutation authority. */
public final class CombatWorldSessions {
    private CombatWorldSessions() {}
    public static java.util.List<io.github.fatalitaya.combatnexus.effect.api.EffectInstance> managedEffects(WorldLookup level, UUID target) {
        return managedEffects(level, target, ignored -> {});
    }
    public static java.util.List<io.github.fatalitaya.combatnexus.effect.api.EffectInstance> managedEffects(WorldLookup level, UUID target, java.util.function.IntConsumer budget) {
        return level.managedEffects(target, budget);
    }
    public static Optional<io.github.fatalitaya.combatnexus.combat.api.source.SourceInstance> source(WorldLookup level, UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId source) {
        return level.source(target, source);
    }
    public static int restrictionMask(WorldLookup level, UUID entity) {
        return level.restrictionMask(entity);
    }
    public static Optional<UUID> current(WorldLookup level) {
        return level.currentSession();
    }
}
