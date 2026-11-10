package io.github.fatalitaya.combatnexus.combat.api.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/** Startup-registered replacement of an unadmitted native root. True consumes the original call.
 * Called synchronously before framework admission; replacements must obtain their own authority.
 * Never called for submitted requests or inside an active combat transaction. */
@FunctionalInterface
public interface NativeDamageIngress {
    boolean replace(LivingEntity target, DamageSource source, float amount);
}
