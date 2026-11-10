package io.github.fatalitaya.combatnexus.combat.damage.builtin.bridge;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModulePreMultiplierBuckets;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;

import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaPreEventScalingBridge;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput;

public final class VanillaDifficultyScalingProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:difficulty_scaling";

    @Override
    public boolean canHandle(NativeDamageContextView context) {
        var ctx = context;

        return VanillaPreEventScalingBridge.canApply(
                ctx,
                NativeDamageInput.DeltaKind.DIFFICULTY_SCALING,
                true
        );
    }

    @Override
    public void apply(NativeDamageRuleContext context) {
        var ctx = context;

        VanillaPreEventScalingBridge.applyApplicationPreMultiplierToAll(
                ctx,
                NativeDamageInput.DeltaKind.DIFFICULTY_SCALING,
                DamageModulePreMultiplierBuckets.runtimeIndex(DamageModulePreMultiplierBuckets.VANILLA_DIFFICULTY),
                DamageModulePreMultiplierBuckets.VANILLA_DIFFICULTY,
                TRACE_ID,
                true,
                DamageApplicationBucket.VANILLA_MELEE_BASE,
                DamageApplicationBucket.VANILLA_MELEE_ENCHANTMENT,
                DamageApplicationBucket.VANILLA_WEAPON_SPECIAL,
                DamageApplicationBucket.VANILLA_PROJECTILE_BASE,
                DamageApplicationBucket.VANILLA_PROJECTILE_ENCHANTMENT,
                DamageApplicationBucket.VANILLA_PROJECTILE_CRIT_BONUS
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.GLOBAL_ADJUSTMENT;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_DIFFICULTY_SCALING;
    }
}
