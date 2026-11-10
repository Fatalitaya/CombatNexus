package io.github.fatalitaya.combatnexus.combat.control.platform;

import io.github.fatalitaya.combatnexus.combat.platform.CombatWorldLifecycle;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The 1.21.1 world clock advances inside tick(), after NeoForge LevelTick.Pre. */
@Mixin(ServerLevel.class)
public abstract class WorldLifecycleMixin implements io.github.fatalitaya.combatnexus.combat.platform.NativeWorldRuntimeBinding {
    @org.spongepowered.asm.mixin.Unique
    private io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession combatnexus$session;
    public io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession combatnexus$worldSession() { return combatnexus$session; }
    public io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding combatnexus$serverBinding() {
        return (io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding)((ServerLevel)(Object)this).getServer();
    }
    public void combatnexus$bindWorldSession(io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession session) {
        if (combatnexus$session != null && session != null) throw new IllegalStateException("World session already bound");
        combatnexus$session = session;
    }

    @Inject(method = "tickTime", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/storage/ServerLevelData;setGameTime(J)V", shift = At.Shift.AFTER))
    private void combatnexus$clock(CallbackInfo ci) {
        CombatWorldLifecycle.clockAdvanced((ServerLevel)(Object)this);
    }
    @Inject(method = "tick", at = @At(value = "CONSTANT", args = "stringValue=tickPending"))
    private void combatnexus$ready(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) {
        CombatWorldLifecycle.tickReady((ServerLevel)(Object)this);
    }
}
