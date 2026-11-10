package io.github.fatalitaya.combatnexus.combat.healing.api;

import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;
import java.util.Optional;

/** Attribution only: does not grant permission to submit or write health. */
public record NativeHealingSource(CombatSource source, HealingRequest.Reason reason) {
    public static final NativeHealingSource UNKNOWN = new NativeHealingSource(
            new CombatSource(NexusId.fromNamespaceAndPath("combatnexus", "unknown_external_healing"),
                    Optional.empty(), Optional.empty()), HealingRequest.Reason.UNKNOWN_EXTERNAL);
    public NativeHealingSource {
        Objects.requireNonNull(source);
        Objects.requireNonNull(reason);
    }
}
