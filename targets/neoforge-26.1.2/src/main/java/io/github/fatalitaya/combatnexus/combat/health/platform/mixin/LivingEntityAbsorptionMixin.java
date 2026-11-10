package io.github.fatalitaya.combatnexus.combat.health.platform.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.fatalitaya.combatnexus.combat.platform.EquipmentCombatAdapter;
import io.github.fatalitaya.combatnexus.combat.api.platform.NativeAbsorptionStorage;
import net.minecraft.world.entity.LivingEntity;
import io.github.fatalitaya.combatnexus.combat.health.platform.NativeAbsorptionTarget;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAbsorptionMixin implements NativeAbsorptionStorage {
    @Unique private NativeAbsorptionStorage.State combatnexus$absorption;
    @Override public NativeAbsorptionStorage.State combatnexus$absorptionState() {
        if (combatnexus$absorption == null) combatnexus$absorption = EquipmentCombatAdapter.nativeDamage().absorptionState(new NativeAbsorptionTarget((LivingEntity)(Object)this));
        return combatnexus$absorption;
    }
    @ModifyReturnValue(method = "getMaxAbsorption()F", at = @At("RETURN"), require = 1)
    private float combatnexus$maximum(float original) {
        var entity = (LivingEntity)(Object)this;
        if (entity.level().isClientSide()) return Math.max(original, entity.getAbsorptionAmount());
        return combatnexus$absorption == null ? original : EquipmentCombatAdapter.nativeDamage().absorptionMaximum(new NativeAbsorptionTarget(entity), combatnexus$absorption, original);
    }
    @Inject(method = "setAbsorptionAmount(F)V", at = @At("TAIL"), require = 1)
    private void combatnexus$consumed(float amount, CallbackInfo callback) {
        if (combatnexus$absorption != null) EquipmentCombatAdapter.nativeDamage().absorptionChanged(new NativeAbsorptionTarget((LivingEntity)(Object)this), combatnexus$absorption);
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combatnexus$save(ValueOutput output, CallbackInfo callback) {
        if (combatnexus$absorption != null) output.putString("combatnexus_absorption",
            EquipmentCombatAdapter.nativeDamage().saveAbsorption(new NativeAbsorptionTarget((LivingEntity)(Object)this), combatnexus$absorption));
    }
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"), require = 1)
    private void combatnexus$restore(ValueInput input, CallbackInfo callback) {
        combatnexus$absorption = EquipmentCombatAdapter.nativeDamage().absorptionState(new NativeAbsorptionTarget((LivingEntity)(Object)this));
        EquipmentCombatAdapter.nativeDamage().restoreAbsorption(new NativeAbsorptionTarget((LivingEntity)(Object)this), combatnexus$absorption,
            input.getString("combatnexus_absorption").orElse("[]"));
    }
}
