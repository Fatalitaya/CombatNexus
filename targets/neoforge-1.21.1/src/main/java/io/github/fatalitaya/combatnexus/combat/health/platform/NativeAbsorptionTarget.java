package io.github.fatalitaya.combatnexus.combat.health.platform;

import io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionTarget;
import io.github.fatalitaya.combatnexus.combat.api.platform.NativeAbsorptionStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** The binding retains exact entity identity, including before world admission. */
public record NativeAbsorptionTarget(LivingEntity entity) implements AbsorptionTarget {
    public NativeAbsorptionTarget { java.util.Objects.requireNonNull(entity); }
    @Override public boolean equals(Object other) { return other instanceof NativeAbsorptionTarget target && entity == target.entity; }
    @Override public int hashCode() { return System.identityHashCode(entity); }
    public void requireThread() {
        if (entity.level() instanceof ServerLevel level && !level.getServer().isSameThread())
            throw new IllegalStateException("Absorption state requires the world thread");
    }
    public NativeAbsorptionStorage.State state() { return ((NativeAbsorptionStorage) entity).combatnexus$absorptionState(); }
    public float amount() { return entity.getAbsorptionAmount(); }
    public void amount(float value) { entity.setAbsorptionAmount(value); }
}
