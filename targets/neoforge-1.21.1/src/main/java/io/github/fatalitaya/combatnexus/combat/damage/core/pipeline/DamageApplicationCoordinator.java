package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

/** Incoming handoff is an application action, never evidence of a native resource commit. */
public final class DamageApplicationCoordinator {
    private DamageApplicationCoordinator() {}
    public static DamageApplicationPlan resolvedAndHandoff(DamageModuleContext context, io.github.fatalitaya.combatnexus.combat.driver.api.ResolvedDamage values) {
        var plan = context.resolvedSummary(values);
        if (context.isManaged()) context.handoff(plan);
        return plan;
    }
    public static DamageApplicationPlan ownershipRejected(DamageModuleContext context) {
        var captured = context.executionSummary();
        var plan = new DamageApplicationPlan(0, java.util.Map.of(), false, captured.criticalDecision(), true, "external_numerical_owner", captured.reactionInputs(), Float.NaN, 0, captured.definitionGeneration(), captured.behaviors(), captured.action(), io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.EMPTY);
        if (context.isManaged()) context.handoff(plan);
        return plan;
    }
    public static DamageApplicationPlan calculateAndHandoff(DamageModuleContext context) {
        DamageApplicationPlan plan = DamageModulePipeline.execute(context);
        if (context.isManaged()) context.handoff(plan);
        return plan;
    }
}
