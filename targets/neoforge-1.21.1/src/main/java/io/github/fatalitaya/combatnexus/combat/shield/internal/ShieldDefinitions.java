package io.github.fatalitaya.combatnexus.combat.shield.internal;
import io.github.fatalitaya.combatnexus.combat.shield.api.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
import io.github.fatalitaya.combatnexus.config.DefinitionJson;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
public final class ShieldDefinitions {
    private ShieldDefinitions() {}
    public static ShieldGrantReceipt grant(CombatRuntime world, UUID target, NexusId definition, CombatSource source,
            double initialBalance, int durationTicks, CombatParent parent) {
        DefinitionJson.range(durationTicks, 1, 1200000, "duration_ticks");
        var value = world.domainDefinitions().shields().get(definition);
        if (value == null) throw new IllegalArgumentException("Unknown shield definition: " + definition);
        return world.shields().replace(target, new ShieldGrant(source, value, initialBalance,
                OptionalLong.of(Math.addExact(world.businessTick(target, source.id()), durationTicks))), parent);
    }
}
