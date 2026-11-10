package io.github.fatalitaya.combatnexus.combat.api;
import java.util.Optional;

/** Lookup never constructs, advances or resurrects a world runtime. */
public final class CombatAccess {
    private CombatAccess() {}
    public static java.util.List<io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCapability> bridgeCoverage() {
        return io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.bridgeCoverage();
    }
    public static Optional<CombatWorld> find(WorldLookup level) {
        return level.combatWorld();
    }
}
