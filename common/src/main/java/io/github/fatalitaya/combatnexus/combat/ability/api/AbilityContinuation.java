package io.github.fatalitaya.combatnexus.combat.ability.api;

import java.util.Objects;

/** Serialized values only; the same registered executor resumes with this state. */
public record AbilityContinuation(int delayTicks, AbilityParameters state) {
    public AbilityContinuation {
        Objects.requireNonNull(state);
        if (delayTicks<1 || delayTicks>1200000) throw new IllegalArgumentException("Invalid ability continuation delay");
    }
}
