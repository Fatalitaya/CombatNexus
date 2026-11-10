package io.github.fatalitaya.combatnexus.combat.healing.platform;

import io.github.fatalitaya.combatnexus.combat.healing.platform.NativeHealingSources;
import io.github.fatalitaya.combatnexus.combat.platform.CombatWorldLifecycle;
import net.minecraft.world.entity.LivingEntity;

public final class NativeHealingAdapter {
    private NativeHealingAdapter() {}
    /** True means the original body must not run, including rejected requests. */
    public static boolean intercept(LivingEntity target, float amount) {
        var source = NativeHealingSources.consume(target);
        if (target.level().isClientSide()) return false;
        // World generation initializes entities on worker threads before world admission.
        // They have no combat session state yet; preserve vanilla initialization.
        if (!target.isAddedToLevel() && !target.isRemoved()
                && target.level() instanceof net.minecraft.server.level.ServerLevel level
                && !level.getServer().isSameThread()) return false;
        return CombatWorldLifecycle.access().heal(target, amount, source);
    }
}
