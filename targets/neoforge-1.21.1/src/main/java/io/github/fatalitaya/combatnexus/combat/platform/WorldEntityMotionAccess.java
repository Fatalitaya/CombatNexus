package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.api.geometry.Vector3;
import io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand.Displace.Mode;
import io.github.fatalitaya.combatnexus.combat.api.platform.EntityMotionAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class WorldEntityMotionAccess implements EntityMotionAccess {
    private final ServerLevel level;
    public WorldEntityMotionAccess(ServerLevel level) { this.level = java.util.Objects.requireNonNull(level); }
    private LivingEntity target(UUID id) { return (LivingEntity) level.getEntity(id); }
    public int fireTicks(UUID id) { return target(id).getRemainingFireTicks(); }
    public void fireTicks(UUID id, int ticks) { target(id).setRemainingFireTicks(ticks); }
    public Vector3 vector(UUID id, Mode mode) {
        var entity = target(id);
        var value = mode == Mode.POSITION ? entity.position() : entity.getDeltaMovement();
        return new Vector3(value.x, value.y, value.z);
    }
    public void knockback(UUID id, double strength, double x, double z) {
        NativeKnockback.apply(target(id), strength, x, z);
    }
    public void vector(UUID id, Mode mode, Vector3 value) {
        var entity = target(id);
        if (mode == Mode.POSITION) entity.teleportTo(value.x(), value.y(), value.z());
        else { entity.setDeltaMovement(new Vec3(value.x(), value.y(), value.z())); entity.hurtMarked = true; }
    }
}
