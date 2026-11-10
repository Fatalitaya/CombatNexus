package io.github.fatalitaya.combatnexus.combat.internal.coordination;

import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import java.util.Optional;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Request planning only; the settlement consumer owns once-only dispatch and the HealingService. */
public final class LifestealRequests {
    private LifestealRequests() {}
    public static io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy policy() {
        var id=NexusId.fromNamespaceAndPath("combatnexus","lifesteal");
        return new io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy(id,0,
            java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE),
            java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.HEALING),c->{
                var s=c.settlement().result(DamageSettlementSnapshot.class).orElseThrow();
                var attacker=io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext.current(s).logicalAttacker();
                var input=attacker==null?null:s.evidence().attributes().get(attacker.getUUID());
                double ratio=input==null?0:input.valueOrZero(io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes.lifesteal());
                var request=c.authority().get().isEmpty()?Optional.<HealingRequest>empty():fromPrimaryHealthLoss(s,id,ratio);
                if(request.isEmpty())return new io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult("ineligible_lifesteal",Optional.empty());
                var result=c.world().healing().submit(request.orElseThrow(),c.authority().get().orElseThrow());
                return new io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult("child_"+result.outcome().name().toLowerCase(java.util.Locale.ROOT),Optional.of("healing/"+result.request().requestId()));
            });
    }

    /** Called only from a registered damage callback; preserves the shared root and its remaining budget. */
    public static Optional<HealingSettlement> submitFrom(
            io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementCallback callback,
            NexusId source, double ratio, HealingService healing) {
        java.util.Objects.requireNonNull(callback, "callback");
        java.util.Objects.requireNonNull(healing, "healing");
        var parent = callback.combatAuthority();
        if (parent.isEmpty()) return Optional.empty();
        return fromPrimaryHealthLoss(callback.snapshot(), source, ratio)
                .map(request -> healing.submit(request, parent.get()));
    }

    /** Default policy excludes derived/reflected damage, absorption and custom shields. */
    public static Optional<HealingRequest> fromPrimaryHealthLoss(DamageSettlementSnapshot settlement,
                                                                NexusId source, double ratio) {
        if (ratio > 1) throw new IllegalArgumentException("Lifesteal ratio exceeds one");
        return Lifesteal.fromPrimaryHealthLoss(settlement, source, ratio);
    }
}
