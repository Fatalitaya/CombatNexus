package io.github.fatalitaya.combatnexus.combat.api.platform;

import java.util.UUID;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.LivingEntity;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance;

/** Native environment inputs and source snapshots; never creates a world or advances its clock. */
public interface NativeEffectsAccess {
    void observedChange(net.minecraft.server.level.ServerLevel level, io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.Effect change);
    boolean projected(NexusId effect);
    SourceProvenance origin(LivingEntity target, UUID entity);
    void sampleEnvironment(LivingEntity entity);
    int remaining(LivingEntity entity, NexusId effect);
    void duration(LivingEntity entity, NexusId effect, int ticks);
}
