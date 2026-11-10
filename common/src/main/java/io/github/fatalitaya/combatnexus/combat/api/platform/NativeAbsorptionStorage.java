package io.github.fatalitaya.combatnexus.combat.api.platform;

/** Entity storage slot for an opaque adapter-owned ledger; it exposes no ledger mutation methods. */
public interface NativeAbsorptionStorage {
    interface State {}
    State combatnexus$absorptionState();
}
