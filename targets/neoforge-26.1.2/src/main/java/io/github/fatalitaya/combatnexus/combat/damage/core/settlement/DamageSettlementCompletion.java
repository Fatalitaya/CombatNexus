package io.github.fatalitaya.combatnexus.combat.damage.core.settlement;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;

import java.util.Objects;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageOrigin;

/** Internal one-shot publication capability for a completed settlement. */
public final class DamageSettlementCompletion {

    private final DamageSettlementSnapshot snapshot;
    private final DamageSettlementState state;
    private final DamageSettlementPublicationGate publicationGate =
            new DamageSettlementPublicationGate();

    DamageSettlementCompletion(
            DamageSettlementSnapshot snapshot,
            DamageSettlementState state
    ) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.state = Objects.requireNonNull(state, "state");
    }

    public DamageSettlementSnapshot snapshot() {
        return snapshot;
    }

    io.github.fatalitaya.combatnexus.combat.api.transaction.ServerSession serverSession() {
        return io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding)state.level().getServer()).orElseThrow().session();
    }
    public boolean matchesOrigin(DamageOrigin origin) { return state.origin() == origin; }
    DamageOrigin origin() { return state.origin(); }
    io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext openContext() {
        return io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext.open(snapshot, state.level(), state.origin(), state.evidence());
    }
    void enqueue(Runnable delivery) { state.enqueue(delivery); }

    boolean claimPublication() {
        if (!publicationGate.claim()) {
            return false;
        }
        state.markPublished();
        return true;
    }

    boolean publicationClaimedForTests() {
        return publicationGate.isClaimed();
    }
}
