package io.github.fatalitaya.combatnexus.combat.api.bridge;

/** Expires with the enclosing CombatRegistrar registration callback. */
@FunctionalInterface
public interface BridgeRegistrar {
    void register(BridgeCapability capability);
}
