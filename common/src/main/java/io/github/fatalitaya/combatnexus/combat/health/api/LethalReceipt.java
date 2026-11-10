package io.github.fatalitaya.combatnexus.combat.health.api;

public record LethalReceipt(Outcome outcome, float healthBefore, float healthAfter) {
    public enum Outcome { MANAGED_PROTECTED, NATIVE_PROTECTED, DEATH_CONFIRMED, DEATH_REFUSED }
}
