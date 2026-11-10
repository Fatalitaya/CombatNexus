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

public final class VanillaProjectileScalingProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:projectile_scaling";

    @Override
    public boolean canHandle(NativeDamageContextView context) {
        var ctx = context;

        return VanillaPreEventScalingBridge.canApply(
                ctx,
                NativeDamageInput.DeltaKind.PROJECTILE_SCALING,
                true
        );
    }

    @Override
    public void apply(NativeDamageRuleContext context) {
        var ctx = context;

        VanillaPreEventScalingBridge.applyApplicationPreMultiplierToAll(
                ctx,
                NativeDamageInput.DeltaKind.PROJECTILE_SCALING,
                DamageModulePreMultiplierBuckets.runtimeIndex(DamageModulePreMultiplierBuckets.VANILLA_PROJECTILE),
                DamageModulePreMultiplierBuckets.VANILLA_PROJECTILE,
                TRACE_ID,
                true,
                DamageApplicationBucket.VANILLA_PROJECTILE_BASE,
                DamageApplicationBucket.VANILLA_PROJECTILE_ENCHANTMENT
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.GLOBAL_ADJUSTMENT;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_PROJECTILE_SCALING;
    }
}
