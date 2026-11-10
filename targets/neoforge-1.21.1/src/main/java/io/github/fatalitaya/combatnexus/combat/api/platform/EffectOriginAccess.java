package io.github.fatalitaya.combatnexus.combat.api.platform;

import io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Supplier;

/** Startup-issued native origin wrapper. Descriptive facts grant no application authority. */
public interface EffectOriginAccess {
    void enchantment(io.github.fatalitaya.combatnexus.api.id.NexusId enchantment, Runnable nativeCall);
    <T> T invoke(LivingEntity target, SourceProvenance provenance, Supplier<T> nativeCall);
}
