package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingSettlement;
import io.github.fatalitaya.combatnexus.combat.shield.api.*;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.LivingEntity;

/** Gameplay-specific executors. Generic dispatch and persistence have no knowledge of these names. */
public final class BuiltinBehaviorReactions {
    private BuiltinBehaviorReactions() {}
    private static ReactionResult skipped(String reason) { return new ReactionResult(reason, Optional.empty()); }
    public static ReactionResult reflect(BuiltinBehaviors.Reflection definition, SettlementContext c) {
        var s = c.settlement().result(DamageSettlementSnapshot.class).orElseThrow();
        var live = io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext.current(s);
        if (definition.ratio() <= 0 || s.requestKind() != DamageRequestKind.PRIMARY || s.status() != DamageSettlementStatus.APPLIED
                || s.cancelled() || !s.evidence().applicationCompleted() || !s.evidence().healthWriteObserved() || s.evidence().healthLoss() <= 0
                || !s.triggerPolicy().reflectionAllowed() || c.damageAuthority().get().isEmpty()
                || live.target() == null || !(live.directEntity() instanceof LivingEntity attacker) || !attacker.isAlive() || attacker.level() != live.level() || attacker == live.target())
            return skipped("ineligible_reflection");
        var limited = c.claim().apply(definition.limit()); if (limited.isPresent()) return skipped(limited.get());
        float amount = (float)(Math.min(s.evidence().healthLoss(), Math.min(s.appliedDamage(), s.healthBefore())) * definition.ratio());
        var request = DamageRequest.builder(live.level(), attacker, DamageSourceDescriptor.of(io.github.fatalitaya.combatnexus.api.id.NexusId.withDefaultNamespace("thorns")), amount)
            .kind(DamageRequestKind.REFLECTED).parent(c.damageAuthority().get().orElseThrow()).logicalAttacker(live.target()).directEntity(live.target())
            .triggerPolicy(DamageTriggerPolicy.NONE_ALLOWED).build();
        var result = c.world().damage(request);
        return new ReactionResult("child_" + result.status().name().toLowerCase(Locale.ROOT), Optional.of("damage/" + request.lineage().damageId()));
    }
    public static ReactionResult reflectBlock(BuiltinBehaviors.ItemBlockReflection definition, SettlementContext c,
            DamageSettlementSnapshot damage, io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockReceipt block) {
        var live = io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext.current(damage);
        var attacker = live.logicalAttacker();
        var blocker = live.target();
        var parent = c.damageAuthority().get();
        if (definition.ratio() <= 0 || !damage.itemBlocked() || damage.requestKind() != DamageRequestKind.PRIMARY
                || !damage.triggerPolicy().reflectionAllowed() || parent.isEmpty()
                || blocker == null || !blocker.isAlive() || attacker == null || !attacker.isAlive() || attacker == blocker)
            return skipped("ineligible_item_block_reflection");
        var limited = c.claim().apply(definition.limit());
        if (limited.isPresent()) return skipped(limited.get());
        var request = DamageRequest.builder(live.level(), attacker,
                DamageSourceDescriptor.of(NexusId.withDefaultNamespace("thorns")), (float)(block.blocked() * definition.ratio()))
            .kind(DamageRequestKind.REFLECTED).parent(parent.orElseThrow()).logicalAttacker(blocker).directEntity(blocker)
            .triggerPolicy(DamageTriggerPolicy.NONE_ALLOWED).build();
        var result = c.world().damage(request);
        return new ReactionResult("child_" + result.status().name().toLowerCase(Locale.ROOT), Optional.of("damage/" + request.lineage().damageId()));
    }
    public static NexusId overflowPool(BehaviorSource source) { return NexusId.fromNamespaceAndPath("combatnexus", "overflow/" + source.installation()); }
    public static ReactionResult overflow(BuiltinBehaviors.OverflowShield definition, SettlementContext c) {
        var s = c.settlement().result(HealingSettlement.class).orElseThrow(); var source = c.source().orElseThrow();
        if (definition.ratio() <= 0 || s.overflow() <= 0 || !s.journal().applicationCompleted()
                || s.outcome() != HealingSettlement.Outcome.APPLIED && s.outcome() != HealingSettlement.Outcome.FULL_HEALTH || c.authority().get().isEmpty())
            return skipped("ineligible_overflow");
        var limited = c.claim().apply(definition.limit()); if (limited.isPresent()) return skipped(limited.get());
        var pool = overflowPool(source);
        double old = c.world().shields().snapshot(source.target()).stream().filter(p -> p.source().id().equals(pool)).mapToDouble(ShieldPoolSnapshot::balance).findFirst().orElse(0);
        double balance = Math.min(definition.capacity(), old + s.overflow() * definition.ratio());
        if (balance <= old) return skipped("shield_capacity");
        long now = c.world().time().now(source.target(), pool);
        var receipt = c.world().shields().grant(UUID.randomUUID(), source.target(), new ShieldGrant(
            new CombatSource(pool, source.owner(), Optional.of(new CombatOperationRef(c.settlement().identity().operation(), io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.HEALING))),
            new ShieldDefinition(definition.capacity(), source.plan().priority(), Set.of(), Set.of(), 0, 0, ShieldDefinition.Stacking.REPLACE_SAME_SOURCE), balance,
            OptionalLong.of(Math.addExact(now, definition.durationTicks()))), c.authority().get().orElseThrow());
        return new ReactionResult(receipt.applied() ? "executed" : "rejected", receipt.value().map(v -> "shield/" + v.operation()));
    }
}
