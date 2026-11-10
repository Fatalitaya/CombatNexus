package io.github.fatalitaya.combatnexus.combat.health.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.fatalitaya.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve native phase/body scaling, while forwarding the actual superclass hit result. */
@Mixin(EnderDragon.class)
public abstract class DragonDamageMixin {
    @Unique private boolean combatnexus$partAccepted;
    @WrapMethod(method="hurt(Lnet/minecraft/world/entity/boss/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    private boolean combatnexus$part(EnderDragonPart part, DamageSource source, float amount, Operation<Boolean> original) {
        boolean previous = combatnexus$partAccepted;
        combatnexus$partAccepted = false;
        try {
            boolean accepted = original.call(part, source, amount) && combatnexus$partAccepted;
            EquipmentCombatAdapter.nativeDamage().partHit(part, (EnderDragon)(Object)this, source, accepted);
            return accepted;
        } finally { combatnexus$partAccepted = previous; }
    }
    @WrapOperation(method="reallyHurt", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Mob;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), require=1)
    private boolean combatnexus$forward(EnderDragon target, DamageSource source, float amount, Operation<Boolean> original) {
        boolean accepted = original.call(target, source, amount);
        combatnexus$partAccepted = accepted;
        return accepted;
    }
}
