package io.github.fatalitaya.combatnexus.combat.healing.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.formula.Amounts;
import java.util.Optional;
import java.util.UUID;

/** Shared eligibility and confirmed-health accounting for settlement-time life steal. */
public final class Lifesteal {
    private Lifesteal() {}
    public static Optional<HealingRequest> fromPrimaryHealthLoss(DamageSettlementSnapshot settlement,
                                                               NexusId source, double ratio) {
        java.util.Objects.requireNonNull(settlement, "settlement");
        java.util.Objects.requireNonNull(source, "source");
        Amounts.nonNegative(ratio, "ratio");
        var live = io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext.current(settlement);
        var attacker = live.logicalAttacker();
        if (!io.github.fatalitaya.combatnexus.combat.healing.internal.LifestealCalculation.eligible(
                settlement.status(),settlement.cancelled(),settlement.requestKind(),settlement.parentDamageId().isPresent(),
                settlement.triggerPolicy().procAllowed(),settlement.evidence().applicationCompleted(),settlement.evidence().healthWriteObserved())
                ||attacker==null||!attacker.isAlive()||attacker.level()!=live.level())return Optional.empty();
        double amount = io.github.fatalitaya.combatnexus.combat.healing.internal.LifestealCalculation.amount(
                settlement.evidence().healthLoss(), settlement.appliedDamage(), settlement.healthBefore(), ratio);
        if (amount <= 0) return Optional.empty();
        CombatSource origin = new CombatSource(source, Optional.of(attacker.getUUID()), Optional.of(new io.github.fatalitaya.combatnexus.combat.api.CombatOperationRef(settlement.damageId(), io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE)));
        return Optional.of(new HealingRequest(UUID.randomUUID(), attacker.getUUID(), origin,
                HealingRequest.Reason.LIFESTEAL, amount));
    }
}
