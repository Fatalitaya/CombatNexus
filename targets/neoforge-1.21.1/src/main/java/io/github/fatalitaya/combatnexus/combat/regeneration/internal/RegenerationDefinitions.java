package io.github.fatalitaya.combatnexus.combat.regeneration.internal;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
public final class RegenerationDefinitions {
    private RegenerationDefinitions() {}
    public static void install(CombatRuntime world, UUID target, NexusId definition, CombatSource source) {
        var value = world.domainDefinitions().regeneration().get(definition);
        if (value == null) throw new IllegalArgumentException("Unknown regeneration definition: " + definition);
        var entity = world.level().getEntity(target);
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || !living.isAlive()) throw new IllegalArgumentException("Recovery requires a live target");
        world.regeneration().replace(new RegenerationPlan(target, source, value,
                OptionalLong.of(Math.addExact(world.businessTick(target, source.id()), value.durationTicks()))));
    }
}
