package io.github.fatalitaya.combatnexus.combat.damage.builtin.bridge;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.VanillaContributionFacts;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput;

public class VanillaArmorEffectivenessProcessor implements DamagePhaseProcessor {

    private static final String TRACE_ID = "vanilla:armor_effectiveness";

    @Override
    public void apply(NativeDamageRuleContext context) {
        var ctx = context;

        if (ctx.nativeInput().armor()
                != NativeDamageInput.Reduction.COMBATNEXUS) return;

        final float multiplier = ctx.nativeInput().armorEffectiveness();

        if (multiplier != 1.0f) {
            DamageMutationResult result = ctx.tryMultiplyArmorEffectiveness(
                    multiplier,
                    TRACE_ID
            );

            ctx.recordContribution(
                    result,
                    VanillaContributionFacts.vanillaArmorEffectiveness(
                            DamageModuleIds.id("vanilla_armor_effectiveness"),
                            DamagePhase.MITIGATION_SETUP,
                            multiplier,
                            TRACE_ID
                    )
            );
        }
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.MITIGATION_SETUP;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_ARMOR_EFFECTIVENESS;
    }
}
