package io.github.fatalitaya.combatnexus.effect.internal;
import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.UUID;
/** Explicit installation; loading a definition never grants an effect. */
public final class EffectDefinitions {
    private EffectDefinitions() {}
    public static boolean install(CombatRuntime world, UUID target, NexusId definition, NexusId source) {
        var value = world.domainDefinitions().effects().get(definition);
        if (value == null) throw new IllegalArgumentException("Unknown effect definition: " + definition);
        return world.effects().apply(value.instantiate(target, definition, source, world.businessTick(target, source)), value.stacking());
    }
}
