package io.github.fatalitaya.combatnexus.combat.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;

/** Frozen identity and provenance, never execution authority. Rejection before admission has no operation identity. */
public record CommandContext(UUID request, UUID target, CombatOperation operation, Optional<CombatSource> source,
        Optional<Identity> identity, Optional<io.github.fatalitaya.combatnexus.combat.api.action.CombatAction> action) {
    public CommandContext {
        Objects.requireNonNull(request); Objects.requireNonNull(target); Objects.requireNonNull(operation);
        Objects.requireNonNull(source); Objects.requireNonNull(identity); Objects.requireNonNull(action);
    }
    public record Identity(long operation, long root, OptionalLong parent, int depth) {}
}
