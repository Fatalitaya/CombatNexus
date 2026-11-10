package io.github.fatalitaya.combatnexus.combat.internal.definition;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
/** Explicit installation entry for equipment, code and commands. Definitions never activate themselves. */
public final class BehaviorEffects {
    private BehaviorEffects() {}
    public static boolean install(CombatRuntime world, UUID target, NexusId definition, NexusId source) {
        var spec = world.behaviorDefinitions().get(definition);
        if (spec == null) throw new IllegalArgumentException("Unknown behavior definition: " + definition);
        return world.effects().apply(new EffectInstance(new EffectSnapshot(target, definition, source, 0,
                spec.lifetime().deadline(world.gameTick()), Set.of(), spec.lifetime().kind()), List.of(),
                io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED, spec.plan()), EffectService.Stacking.REPLACE);
    }
}
