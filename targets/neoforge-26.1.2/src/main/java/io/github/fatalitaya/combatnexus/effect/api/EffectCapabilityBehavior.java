package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;

/** The capability ID is persisted with the adopted plan and dispatched by the common scheduler. */
public record EffectCapabilityBehavior(NexusId capability, int initialDelayTicks) implements PeriodicBehavior {
    public EffectCapabilityBehavior(NexusId capability) { this(capability, 1); }
    public EffectCapabilityBehavior { java.util.Objects.requireNonNull(capability); if (initialDelayTicks < 0) throw new IllegalArgumentException("Negative initial delay"); }
    public int intervalTicks() { return 1; }
    public PeriodicPhase phase() { return PeriodicPhase.PRESERVE; }
    public void execute(PeriodicExecution execution) { execution.effectCapability(capability); }
}
