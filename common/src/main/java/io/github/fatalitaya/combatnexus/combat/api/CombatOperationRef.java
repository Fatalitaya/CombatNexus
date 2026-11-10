package io.github.fatalitaya.combatnexus.combat.api;

import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatLineage;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import java.util.Objects;

/** Explanation only. Never usable as admission or child-operation authority. */
public record CombatOperationRef(long id, CombatOperation operation) {
    public CombatOperationRef {
        if (id < 1) throw new IllegalArgumentException("Invalid operation identity");
        Objects.requireNonNull(operation);
    }
    public static CombatOperationRef of(CombatLineage lineage) {
        return new CombatOperationRef(lineage.id(), lineage.operation());
    }
}
