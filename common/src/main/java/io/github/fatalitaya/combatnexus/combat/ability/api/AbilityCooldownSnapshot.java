package io.github.fatalitaya.combatnexus.combat.ability.api;

import java.util.*;

public record AbilityCooldownSnapshot(UUID actor, AbilityCooldownSlot slot, long remaining, Optional<UUID> execution) {
    public AbilityCooldownSnapshot { Objects.requireNonNull(actor); Objects.requireNonNull(slot); Objects.requireNonNull(execution); if(remaining<0)throw new IllegalArgumentException("Negative remaining time"); }
    public record Change(AbilityCooldownSnapshot before, AbilityCooldownSnapshot after) {
        public Change { Objects.requireNonNull(before); Objects.requireNonNull(after); }
    }
}
