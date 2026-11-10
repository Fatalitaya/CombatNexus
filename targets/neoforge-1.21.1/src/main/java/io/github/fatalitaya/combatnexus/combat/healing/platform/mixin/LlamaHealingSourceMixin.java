package io.github.fatalitaya.combatnexus.combat.healing.platform.mixin;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fatalitaya.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.horse.Llama")
public abstract class LlamaHealingSourceMixin {
    @WrapOperation(method = "handleEating", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/horse/Llama;heal(F)V"), require = 1)
    private void combatnexus$handleEating(net.minecraft.world.entity.animal.horse.Llama target, float amount, Operation<Void> original, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack item) {
        NativeHealingCalls.call(target, amount, original, NativeIds.nexus(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem())), HealingRequest.Reason.ITEM, player);
    }
}
