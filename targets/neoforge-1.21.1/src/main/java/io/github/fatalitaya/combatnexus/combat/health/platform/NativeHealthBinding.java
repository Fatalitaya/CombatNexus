package io.github.fatalitaya.combatnexus.combat.health.platform;

import io.github.fatalitaya.combatnexus.combat.api.platform.HealthTarget;
import net.minecraft.world.entity.LivingEntity;

/** All bindings of one exact entity share the contention identity. */
public class NativeHealthBinding implements HealthTarget {
    protected final LivingEntity entity;
    public NativeHealthBinding(LivingEntity entity) { this.entity = java.util.Objects.requireNonNull(entity); }
    @Override public final boolean equals(Object other) { return other instanceof NativeHealthBinding binding && entity == binding.entity; }
    @Override public final int hashCode() { return System.identityHashCode(entity); }
    public float health() { return entity.getHealth(); }
    public float maximumHealth() { return entity.getMaxHealth(); }
    public void health(float value) { entity.setHealth(value); }
}
