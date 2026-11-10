package io.github.fatalitaya.combatnexus.combat.api.platform;

/** Exact entity identity: repeated bindings of the same entity must compare equal. */
public interface AbsorptionTarget {
    void requireThread();
    NativeAbsorptionStorage.State state();
    float amount();
    void amount(float value);
}
