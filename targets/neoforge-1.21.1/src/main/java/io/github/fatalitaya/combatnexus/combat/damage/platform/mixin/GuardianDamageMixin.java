package io.github.fatalitaya.combatnexus.combat.damage.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The registered settlement policy owns guardian retaliation after resource consumption. */
@Mixin(Guardian.class)
public abstract class GuardianDamageMixin {
    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), require = 1)
    private boolean combatnexus$deferThorns(LivingEntity attacker, DamageSource source,
            float amount, Operation<Boolean> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) DamageSource incoming) {
        if (!io.github.fatalitaya.combatnexus.combat.platform.EquipmentCombatAdapter.nativeDamage().managed(incoming))
            return original.call(attacker, source, amount);
        return false;
    }
}
