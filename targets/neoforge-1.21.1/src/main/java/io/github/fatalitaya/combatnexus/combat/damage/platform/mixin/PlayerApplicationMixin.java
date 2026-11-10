package io.github.fatalitaya.combatnexus.combat.damage.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fatalitaya.combatnexus.combat.platform.DamageApplicationAdapter;
import io.github.fatalitaya.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerApplicationMixin {
    @WrapOperation(method = "attack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), require = 1)
    private boolean combatnexus$sweep(LivingEntity target, DamageSource source, float amount, Operation<Boolean> original) {
        return EquipmentCombatAdapter.nativeDamage().sweep(target, source, () -> original.call(target, source, amount));
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "attack")
    private void combatnexus$action(net.minecraft.world.entity.Entity target, Operation<Void> original) {
        EquipmentCombatAdapter.nativeDamage().attack((Player)(Object)this, target, () -> original.call(target));
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/neoforge/common/CommonHooks;fireCriticalHit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;ZF)Lnet/neoforged/neoforge/event/entity/player/CriticalHitEvent;"), require = 1)
    private net.neoforged.neoforge.event.entity.player.CriticalHitEvent combatnexus$critOwnership(
            Player player, net.minecraft.world.entity.Entity target, boolean critical, float multiplier,
            Operation<net.neoforged.neoforge.event.entity.player.CriticalHitEvent> original,
            @com.llamalad7.mixinextras.sugar.Local DamageSource source) {
        return EquipmentCombatAdapter.nativeDamage().attackOwnership(source, () -> original.call(player, target, critical, multiplier));
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F"), require = 1)
    private float combatnexus$shieldBoundary(LivingEntity target, DamageContainer container, Operation<Float> original) {
        return DamageApplicationAdapter.consumeShields(target, container, original.call(target, container));
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"), require = 1)
    private void combatnexus$healthBoundary(Player target, float value, Operation<Void> original,
                                          DamageSource source, float amount) {
        EquipmentCombatAdapter.nativeDamage().writeHealth(target, source, value, next -> original.call(target, next));
    }
}
