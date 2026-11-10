package io.github.fatalitaya.combatnexus.combat.ability.api;

import java.util.*;
import java.util.function.*;

/** Startup declaration. Availability is evaluated under the kernel's read-only compute guard. */
public record AbilityExecutor(AbilityExecutorSpec spec, Predicate<AbilityInput> available,
                              Function<AbilityExecution, Optional<AbilityContinuation>> execute) {
    public AbilityExecutor { Objects.requireNonNull(spec); Objects.requireNonNull(available); Objects.requireNonNull(execute); }
}
