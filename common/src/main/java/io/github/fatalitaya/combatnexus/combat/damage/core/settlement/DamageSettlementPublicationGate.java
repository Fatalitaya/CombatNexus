package io.github.fatalitaya.combatnexus.combat.damage.core.settlement;

import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatPublicationGate;

final class DamageSettlementPublicationGate {
    private final CombatPublicationGate gate = new CombatPublicationGate();
    boolean claim() { return gate.claim(); }
    boolean isClaimed() { return gate.isClaimed(); }
}
