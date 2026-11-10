package probe;

import net.minecraft.world.entity.LivingEntity;
import java.util.concurrent.CompletableFuture;

/** Executes the native call on a worker without reentering AutoJS's server-thread executor. */
public final class WorldGenerationHealing {
    private WorldGenerationHealing() {}
    public static CompletableFuture<Void> heal(LivingEntity entity, float amount) {
        return CompletableFuture.runAsync(() -> entity.heal(amount));
    }
}
