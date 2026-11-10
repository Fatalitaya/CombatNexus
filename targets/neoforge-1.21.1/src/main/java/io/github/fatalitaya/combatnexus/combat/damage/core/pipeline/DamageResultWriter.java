package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

/** Scoped platform writeback. Implementations retain event/container identity. */
@FunctionalInterface
public interface DamageResultWriter {
    void applyIncomingDamage(DamageModuleContext context, DamageApplicationPlan plan);
}
