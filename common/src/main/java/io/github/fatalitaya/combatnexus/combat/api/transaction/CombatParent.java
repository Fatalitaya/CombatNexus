package io.github.fatalitaya.combatnexus.combat.api.transaction;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope;

import java.util.Set;

/** Opaque callback-scoped authority; retaining it never extends its lifetime. */
public final class CombatParent {
    final ServerSession server;
    final CombatLineage lineage;
    final Set<CombatOperation> allowed;
    public CombatParent(ServerSession server, CombatLineage lineage, Set<CombatOperation> allowed) {
        this.server = server;
        this.lineage = lineage;
        this.allowed = Set.copyOf(allowed);
    }
    public CombatLineage lineage() { return lineage; }
    public boolean permits(ServerSession server, CombatOperation operation) {
        return CombatDispatchScope.permits(this, server, operation) && allowed.contains(operation);
    }
    public CombatLineage child(ServerSession server, CombatOperation operation) {
        if (!permits(server, operation)) throw new IllegalStateException("Combat parent authority is inactive or restricted");
        return lineage.child(operation, allowed);
    }
}
