package io.github.fatalitaya.combatnexus.combat.damage.api.context;

/**
 * Public rule-facing context.
 * <p>
 * Rules can inspect stable damage state through DamageContextView and mutate
 * damage only through DamageMutationContext.
 */
public interface DamageRuleContext
        extends DamageContextView, DamageMutationContext {
    /** Applies ratio then flat penetration before built-in penetration and mitigation.
     * Only MITIGATION_SETUP, before the selected input is consumed, and only for
     * CombatNexus-owned reduction. Each accepted contribution records its frozen source facts.
     */
    DamageMutationResult tryPenetrateDefense(DefenseInput input, float ratio, float flat,
            io.github.fatalitaya.combatnexus.api.id.NexusId source);
    /** Records the result of a guarded mutation during this calculation callback. */
    void recordContribution(DamageMutationResult result,
            io.github.fatalitaya.combatnexus.combat.damage.api.display.ContributionRecord fact);
    DamageMutationResult tryAddVanillaBaseReconstructedDamage(
            io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel channel,
            io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket bucket, float value, String sourceId);
}
