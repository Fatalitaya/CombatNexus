package io.github.fatalitaya.combatnexus.combat.api.behavior;
import java.util.OptionalDouble;
/** A decision before the health write. Only an effective decision consumes a use. */
public interface ProtectionBehavior extends BehaviorComponent {
    OptionalDouble protect(ProtectionContext context);
    default int maximumUses() { return 1; }
    /** Zero keeps exhausted uses consumed for this installation; positive values refill after the last trigger. */
    default int cooldownTicks() { return 0; }
}
