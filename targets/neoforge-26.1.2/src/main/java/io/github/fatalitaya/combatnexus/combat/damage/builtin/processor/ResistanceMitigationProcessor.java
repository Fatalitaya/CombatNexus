package io.github.fatalitaya.combatnexus.combat.damage.builtin.processor;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.core.DamageComponent;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageInternalContexts;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamagePhaseAccess;
import io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes;
import io.github.fatalitaya.combatnexus.combat.damage.api.VanillaDamageTags;

import java.util.Objects;

public class ResistanceMitigationProcessor implements DamagePhaseProcessor {
    @Override
    public void apply(NativeDamageRuleContext context) {
        DamagePhaseAccess ctx = DamageInternalContexts.phase(
                context,
                "phase processor"
        );

        for (int i = 0; i < ctx.getActiveComponentCount(); i++) {
            DamageComponent component = ctx.getActiveComponent(i);

            float currentDmg = component.getPostMitigationAmount();
            if (currentDmg <= 0.0f) {
                continue;
            }

            NexusId channelAttribute =
                    ctx.definitions().damage().channels().data(component.channel).resistanceAttribute();
            NexusId categoryAttribute = categoryAttribute(ctx);

            float channelRating =
                    channelAttribute != null
                            ? finiteOrZero(ctx.getVictimAttrOrZero(channelAttribute))
                            : 0.0f;

            float tempRating = finiteOrZero(
                    component.getTemporaryResistanceRating());

            float categoryRating = categoryAttribute != null
                    && !sameAttribute(channelAttribute, categoryAttribute)
                    ? finiteOrZero(ctx.getVictimAttrOrZero(categoryAttribute))
                    : 0.0f;

            float totalRating = totalRating(
                    channelRating, tempRating, categoryRating);

            if (totalRating == 0.0f) {
                continue;
            }

            float kValue = Math.max(
                    0.0001f,
                    ctx.settings().values().formulas().resistanceKValue()
            );

            float reduction = reductionFor(totalRating, kValue);

            ctx.tryAddChannelMitigation(
                    component.channel,
                    reduction,
                    "dn:resistance"
            );

        }
    }

    @Override
    public boolean canHandle(NativeDamageContextView context) {
        DamagePhaseAccess ctx = DamageInternalContexts.phase(
                context,
                "phase processor predicate"
        );

        return shouldMitigate(
                ctx.isManaged(),
                io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().matchesDamageTag(ctx.source(), VanillaDamageTags.BYPASSES_EFFECTS),
                io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().matchesDamageTag(ctx.source(), VanillaDamageTags.BYPASSES_RESISTANCE)
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.MITIGATION_SETUP;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.DN_RESISTANCE_MITIGATION;
    }

    static float finiteOrZero(float value) {
        return io.github.fatalitaya.combatnexus.formula.ResistanceFormula.finiteOrZero(value);
    }

    static float totalRating(
            float channelRating,
            float temporaryRating,
            float categoryRating
    ) {
        return io.github.fatalitaya.combatnexus.formula.ResistanceFormula.totalRating(channelRating, temporaryRating, categoryRating);
    }

    /**
     * Positive ratings use the asymptotic rating / (rating + K) formula and
     * cap at 95% reduction. Negative ratings use rating / K, so they express
     * vulnerability without a gameplay-scale negative reduction cap.
     */
    static float reductionFor(float rating, float kValue) {
        return io.github.fatalitaya.combatnexus.formula.ResistanceFormula.reductionFor(rating, kValue);
    }

    static boolean sameAttribute(
            NexusId first,
            NexusId second
    ) {
        return sameReference(first, second);
    }

    static boolean shouldMitigate(
            boolean managed,
            boolean bypassesEffects,
            boolean bypassesResistance
    ) {
        return managed && !bypassesEffects && !bypassesResistance;
    }

    static boolean sameReference(Object first, Object second) {
        return first != null && second != null
                && (first == second || Objects.equals(first, second));
    }

    private static NexusId categoryAttribute(
            DamagePhaseAccess context
    ) {
        if (context.isProjectileDamage()) {
            return CombatAttributes.resistanceProjectile();
        }
        if (context.isMeleeDamage()) {
            return CombatAttributes.resistanceMelee();
        }
        return null;
    }
}

