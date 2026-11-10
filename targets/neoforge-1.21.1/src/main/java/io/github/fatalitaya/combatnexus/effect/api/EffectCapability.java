package io.github.fatalitaya.combatnexus.effect.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/** Registered platform behavior; return false to end the active effect after a tick. */
@FunctionalInterface
public interface EffectCapability {
    boolean execute(Context context);
    default java.util.List<io.github.fatalitaya.combatnexus.presentation.api.DisplayText> describe(int amplifier) {
        return java.util.List.of(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.effect.unknown_behavior"));
    }
    enum Trigger { APPLY, TICK, HURT, DEATH, END }
    record Context(LivingEntity entity, EffectInstance effect, Trigger trigger, DamageSource damageSource, float damage) {}
}
