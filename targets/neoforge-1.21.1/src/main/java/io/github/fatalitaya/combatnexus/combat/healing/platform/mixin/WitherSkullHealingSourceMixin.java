package io.github.fatalitaya.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fatalitaya.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.projectile.WitherSkull")
public abstract class WitherSkullHealingSourceMixin {
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"), require = 1)
    private void combatnexus$onHitEntity(net.minecraft.world.entity.LivingEntity target, float amount, Operation<Void> original) {
        NativeHealingCalls.call(target, amount, original, NexusId.withDefaultNamespace("wither_skull_healing"), HealingRequest.Reason.SKILL, target);
    }
}
