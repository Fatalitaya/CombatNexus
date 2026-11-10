package io.github.fatalitaya.combatnexus.combat.ability.api;

import java.util.Objects;

/** Exactly one next action after a callback. Input expiry never invokes the executor. */
public sealed interface AbilityStepResult {
    record Complete() implements AbilityStepResult {}
    record Delay(AbilityContinuation continuation) implements AbilityStepResult {
        public Delay { Objects.requireNonNull(continuation); }
    }
    record AwaitInput(int ticks,int attempts,AbilityParameters state) implements AbilityStepResult {
        public AwaitInput { Objects.requireNonNull(state); if(ticks<1||ticks>1200000||attempts<1||attempts>63)throw new IllegalArgumentException("Invalid input window"); }
    }
}
