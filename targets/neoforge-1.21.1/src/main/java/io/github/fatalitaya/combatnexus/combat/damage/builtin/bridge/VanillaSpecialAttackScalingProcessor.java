package io.github.fatalitaya.combatnexus.combat.damage.builtin.bridge;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;

import io.github.fatalitaya.combatnexus.combat.damage.api.display.VanillaContributionFacts;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput;

import java.util.Locale;

public final class VanillaSpecialAttackScalingProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;
    private static final String TRACE_ID = "vanilla:special_attack_bonus";

    @Override
    public boolean canHandle(NativeDamageContextView context) {
        var ctx = context;

        if (!ctx.nativeInput().rebuildPreEvent()) {
            return false;
        }

        NativeDamageInput.Offense snapshot =
                ctx.nativeInput().offense().orElse(null);

        if (snapshot == null) {
            return false;
        }

        NativeDamageInput.Delta delta =
                snapshot.preEventDelta();

        return delta.kind() == NativeDamageInput.DeltaKind.SPECIAL_ATTACK_SCALING
                && Float.isFinite(delta.delta())
                && Math.abs(delta.delta()) > EPSILON;
    }

    @Override
    public void apply(NativeDamageRuleContext context) {
        var ctx = context;

        NativeDamageInput.Delta delta =
                ctx.nativeInput().offense().orElse(null).preEventDelta();

        DamageApplicationBucket bucket =
                DamageApplicationBucket.VANILLA_WEAPON_SPECIAL;

        DamageMutationResult result = ctx.tryAddVanillaBaseReconstructedDamage(
                ctx.getInitialChannel(),
                bucket,
                delta.delta(),
                TRACE_ID
        );

        ctx.recordContribution(
                result,
                VanillaContributionFacts.vanillaBase(
                        DamageModuleIds.id(
                                "vanilla_special_attack/"
                                        + bucket.name().toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        bucket,
                        delta.delta(),
                        TRACE_ID
                )
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_WEAPON_SPECIAL_BONUS;
    }
}
