package io.github.fatalitaya.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fatalitaya.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.food.FoodData")
public abstract class FoodDataHealingSourceMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"), require = 2)
    private void combatnexus$tick(net.minecraft.server.level.ServerPlayer target, float amount, Operation<Void> original) {
        NativeHealingCalls.call(target, amount, original, NexusId.withDefaultNamespace("food_regeneration"), HealingRequest.Reason.NATURAL_REGENERATION, target);
    }
}
