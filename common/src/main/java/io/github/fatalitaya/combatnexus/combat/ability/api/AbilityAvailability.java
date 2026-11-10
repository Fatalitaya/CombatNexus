package io.github.fatalitaya.combatnexus.combat.ability.api;

import java.util.*;

public record AbilityAvailability(boolean available, Reason reason, long cooldownRemaining, Optional<AbilityInput> input) {
    public enum Reason { AVAILABLE, STALE_GRANT, SOURCE_INACTIVE, TARGET_UNAVAILABLE, TARGET_CONTRACT, COOLDOWN, RESTRICTED, EXTERNAL_OWNER, EXECUTOR_REFUSED, CAPTURE_FAILED }
    public AbilityAvailability {
        Objects.requireNonNull(reason); Objects.requireNonNull(input);
        if (cooldownRemaining<0 || available!=(reason==Reason.AVAILABLE)) throw new IllegalArgumentException("Invalid availability");
    }
}
