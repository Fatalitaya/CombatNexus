package io.github.fatalitaya.combatnexus.combat.damage.api.context;

/** Native extension callbacks may additionally plan typed application requests. */
public interface NativeDamageRuleContext extends DamageRuleContext, NativeDamageContextView {
    /** Freeze a CombatNexus-owned defense input during MITIGATION_SETUP before applying its reduction. */
    float consumeDefenseInput(DefenseInput input);
    void beforeApplication(io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Step step);
}
