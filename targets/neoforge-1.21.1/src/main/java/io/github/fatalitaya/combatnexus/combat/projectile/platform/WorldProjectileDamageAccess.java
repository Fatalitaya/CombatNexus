package io.github.fatalitaya.combatnexus.combat.projectile.platform;

import io.github.fatalitaya.combatnexus.combat.projectile.api.*;
import net.minecraft.server.level.ServerLevel;

public record WorldProjectileDamageAccess(ServerLevel level) implements ProjectileDamageWorldAccess {
    public WorldProjectileDamageAccess { java.util.Objects.requireNonNull(level); }
    public boolean available(ProjectileDamageTarget target) {
        return target instanceof NativeArrowDamageTarget nativeTarget
                && nativeTarget.arrow().level() == level && !nativeTarget.arrow().isRemoved();
    }
    public long gameTick() { return level.getGameTime(); }
}
