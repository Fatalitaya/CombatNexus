package io.github.fatalitaya.combatnexus.combat.time.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CombatAccess;
import io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Reads an existing session only. Client and pre-session observations use the default clock. */
public final class NativeBusinessTime {
    private NativeBusinessTime() {}
    public static long phase(LivingEntity entity, NexusId source) {
        if (entity.level() instanceof ServerLevel level) {
            var world=CombatAccess.find((WorldRuntimeBinding)level);
            if(world.isPresent())return world.get().time().phase(entity.getUUID(),source,entity.tickCount);
        }
        return entity.tickCount;
    }

    public static long now(LivingEntity entity, NexusId source) {
        if (entity.level() instanceof ServerLevel level) {
            var world = CombatAccess.find((WorldRuntimeBinding) level);
            if (world.isPresent()) return world.get().time().now(entity.getUUID(), source);
        }
        return entity.level().getGameTime();
    }
}
