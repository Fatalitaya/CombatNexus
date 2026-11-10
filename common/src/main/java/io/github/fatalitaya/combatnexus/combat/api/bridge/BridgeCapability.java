package io.github.fatalitaya.combatnexus.combat.api.bridge;

import java.util.Objects;
import java.util.Set;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;

/** A startup declaration, not a grant of execution authority. */
public record BridgeCapability(NexusId id, NexusId trigger, NexusId position,
        NexusId executor, Mode mode, Set<CombatOperation> permissions, String description) {
    public enum Mode { OBSERVE, ROUTE, REPLACE, NATIVE }
    public BridgeCapability {
        Objects.requireNonNull(id); Objects.requireNonNull(trigger); Objects.requireNonNull(position);
        Objects.requireNonNull(executor); Objects.requireNonNull(mode); Objects.requireNonNull(description);
        permissions = Set.copyOf(permissions);
        if ((mode == Mode.OBSERVE || mode == Mode.NATIVE) && !permissions.isEmpty())
            throw new IllegalArgumentException("Observation/native declarations cannot grant commands");
    }
}
