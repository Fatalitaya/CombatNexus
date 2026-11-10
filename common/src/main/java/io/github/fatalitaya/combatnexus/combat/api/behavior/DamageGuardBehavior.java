package io.github.fatalitaya.combatnexus.combat.api.behavior;

import io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan;
import java.util.Optional;
import java.util.OptionalDouble;

/** Selects one protection in stable source order, consuming only the selected installation.
 * Selection is pure computation. Its commands execute afterwards in APPLICATION; failures
 * retain consumption and confirmed writes and stop the parent application. */
public interface DamageGuardBehavior extends ProtectionBehavior {
    enum Stage { BEFORE_RESOURCES, REMAINING_HEALTH }
    Stage stage();
    /** Empty declines. An empty plan still selects and consumes this protection.
     * BEFORE_RESOURCES cancels all remaining damage; REMAINING_HEALTH reassigns damage
     * after the commands, allowing a newly granted shield to absorb this same request. */
    Optional<BeforeApplicationPlan> select(DamageGuardContext context);
    @Override default OptionalDouble protect(ProtectionContext context) { return OptionalDouble.empty(); }
}
