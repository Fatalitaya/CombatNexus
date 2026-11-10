package io.github.fatalitaya.combatnexus.combat.control.platform;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatActionGate;
import static io.github.fatalitaya.combatnexus.effect.api.EffectSnapshot.RestrictedAction.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Mob.class)
public abstract class MobControlMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void combatnexus$attack(Entity target, CallbackInfoReturnable<Boolean> ci) {
        if (!CombatActionGate.allows((LivingEntity)(Object)this, ATTACK)) ci.setReturnValue(false);
    }
}
