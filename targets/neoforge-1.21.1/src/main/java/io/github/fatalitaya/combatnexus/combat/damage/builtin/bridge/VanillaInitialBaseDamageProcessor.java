package io.github.fatalitaya.combatnexus.combat.damage.builtin.bridge;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.VanillaContributionFacts;

import java.util.Locale;

public final class VanillaInitialBaseDamageProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;

    @Override
    public void apply(NativeDamageRuleContext context) {
        var ctx = context;

        String traceId = "vanilla:initial_base/"
                + ctx.nativeInput().initialBaseBucket().name().toLowerCase(Locale.ROOT);

        DamageMutationResult result = ctx.tryAddBaseDamage(
                ctx.getInitialChannel(),
                ctx.nativeInput().initialBaseBucket(),
                ctx.nativeInput().initialBaseAmount(),
                traceId
        );

        ctx.recordContribution(
                result,
                VanillaContributionFacts.vanillaBase(
                        DamageModuleIds.id(
                                "vanilla_initial_base/"
                                        + ctx.nativeInput().initialBaseBucket()
                                                .name()
                                                .toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.nativeInput().initialBaseBucket(),
                        ctx.nativeInput().initialBaseAmount(),
                        traceId
                )
        );
    }

    @Override
    public boolean canHandle(NativeDamageContextView context) {
        var ctx = context;

        return ctx.nativeInput().initialBaseAmount() > EPSILON;
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_INITIAL_BASE;
    }
}
