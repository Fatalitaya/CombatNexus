package io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla;
import io.github.fatalitaya.combatnexus.effect.api.VanillaEffectContributions;
import io.github.fatalitaya.combatnexus.effect.api.VanillaEffectContributions.OffensiveMobEffectBreakdown;

public final class VanillaMobEffectBridge {
    private VanillaMobEffectBridge() {}
    public static OffensiveMobEffectBreakdown computeOffensiveBreakdown(VanillaDamageSourceProfile profile) {
        if (profile == null || profile.offensiveEffects().isEmpty() || !profile.shouldApplyMeleeOffensiveMobEffects()) {
            return OffensiveMobEffectBreakdown.NONE;
        }
        return VanillaEffectContributions.computeBreakdown(profile.offensiveEffects().orElseThrow());
    }
}
