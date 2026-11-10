package io.github.fatalitaya.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fatalitaya.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.player.Player")
public abstract class PlayerHealingSourceMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"), require = 1)
    private void combatnexus$aiStep(net.minecraft.world.entity.player.Player target, float amount, Operation<Void> original) {
        NativeHealingCalls.call(target, amount, original, NexusId.withDefaultNamespace("peaceful_regeneration"), HealingRequest.Reason.NATURAL_REGENERATION, target);
    }
}
