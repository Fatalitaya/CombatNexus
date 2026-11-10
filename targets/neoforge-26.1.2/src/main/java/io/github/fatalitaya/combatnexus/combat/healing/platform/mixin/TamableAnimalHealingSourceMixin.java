package io.github.fatalitaya.combatnexus.combat.healing.platform.mixin;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fatalitaya.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.TamableAnimal")
public abstract class TamableAnimalHealingSourceMixin {
    @WrapOperation(method = "feed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;heal(F)V"), require = 1)
    private void combatnexus$feed(net.minecraft.world.entity.TamableAnimal target, float amount, Operation<Void> original, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack item, float factor, float defaultHeal) {
        NativeHealingCalls.call(target, amount, original, NativeIds.nexus(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem())), HealingRequest.Reason.ITEM, player);
    }
}
