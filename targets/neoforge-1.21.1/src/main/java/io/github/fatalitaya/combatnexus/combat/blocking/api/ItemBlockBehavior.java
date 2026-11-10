package io.github.fatalitaya.combatnexus.combat.blocking.api;

import io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementBehavior;
import io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementContext;
import io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import java.util.Optional;
import java.util.Set;

/** Register with CombatRegistrar.behavior and install through an Effect or equipment binding.
 * Called after the enclosing damage call, including full blocks with no successful Hit.
 * Declare child permissions as for any SettlementBehavior; use the context's expiring
 * authorities for damage/healing/effects. Observers of DamageSettledEvent remain read-only.
 */
public interface ItemBlockBehavior extends SettlementBehavior {
    @Override
    default Set<CombatOperation> subscriptions() { return Set.of(CombatOperation.DAMAGE); }

    @Override
    default ReactionResult settle(SettlementContext context) {
        var damage = context.settlement().result(DamageSettlementSnapshot.class).orElseThrow();
        if (!damage.itemBlocked()) return new ReactionResult("no_completed_item_block", Optional.empty());
        return afterBlock(context, damage, damage.evidence().itemBlock().orElseThrow());
    }

    ReactionResult afterBlock(SettlementContext context, DamageSettlementSnapshot damage, ItemBlockReceipt block);
}
