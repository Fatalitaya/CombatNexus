package io.github.fatalitaya.combatnexus.combat.api.behavior;

import io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit;
import io.github.fatalitaya.combatnexus.config.DefinitionJson;
import java.util.Objects;

/** Built-in executable behaviors, wired by the platform composition root. */
public final class BuiltinBehaviors {
    private BuiltinBehaviors() {}
    public record LethalProtection(int cooldownTicks) implements ProtectionBehavior {
        public LethalProtection() { this(0); }
        public LethalProtection { DefinitionJson.range(cooldownTicks, 0, 1200000, "cooldown_ticks"); } public java.util.OptionalDouble protect(ProtectionContext c) { return c.before() > 0 && c.proposed() <= 0 ? java.util.OptionalDouble.of(Math.min(c.before(), 1.0f)) : java.util.OptionalDouble.empty(); } }
    public record Reflection(double ratio, TriggerLimit limit) implements SettlementBehavior {
        public java.util.Set<io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation> permissions() { return java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE); }
        public java.util.Set<io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation> subscriptions() { return java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE); }
        public io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult settle(SettlementContext context) { return io.github.fatalitaya.combatnexus.combat.internal.coordination.BuiltinBehaviorReactions.reflect(this, context); }
        public Reflection { DefinitionJson.range(ratio, 0, 1, "ratio"); Objects.requireNonNull(limit); }
    }
    /** Reflects the blocked amount to the logical attacker, including projectile shooters. */
    public record ItemBlockReflection(double ratio, TriggerLimit limit) implements io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockBehavior {
        public ItemBlockReflection { DefinitionJson.range(ratio, 0, 1, "ratio"); Objects.requireNonNull(limit); }
        public java.util.Set<io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation> permissions() {
            return java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE);
        }
        public io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult afterBlock(SettlementContext context,
                io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot damage,
                io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockReceipt block) {
            return io.github.fatalitaya.combatnexus.combat.internal.coordination.BuiltinBehaviorReactions.reflectBlock(this, context, damage, block);
        }
    }
    public record OverflowShield(double ratio, double capacity, int durationTicks, TriggerLimit limit) implements SettlementBehavior {
        public java.util.Set<io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation> permissions() { return java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.SHIELD); }
        public java.util.Set<io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation> subscriptions() { return java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.HEALING); }
        public io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult settle(SettlementContext context) { return io.github.fatalitaya.combatnexus.combat.internal.coordination.BuiltinBehaviorReactions.overflow(this, context); }
        public java.util.Map<io.github.fatalitaya.combatnexus.api.id.NexusId, io.github.fatalitaya.combatnexus.api.id.NexusId> ownedShields(BehaviorSource source) { return java.util.Map.of(type(), io.github.fatalitaya.combatnexus.combat.internal.coordination.BuiltinBehaviorReactions.overflowPool(source)); }
        public OverflowShield { DefinitionJson.range(ratio, 0, 1, "ratio"); DefinitionJson.range(capacity, 0, 1000000, "capacity");
            DefinitionJson.range(durationTicks, 1, 1200000, "duration_ticks"); Objects.requireNonNull(limit); }
    }
    public record PeriodicHealing(double amount, int intervalTicks, int firstDelayTicks, boolean onlyWhenMissingHealth, PeriodicPhase phase) implements PeriodicBehavior {
        public PeriodicHealing(double amount, int intervalTicks, int firstDelayTicks, boolean onlyWhenMissingHealth) {
            this(amount, intervalTicks, firstDelayTicks, onlyWhenMissingHealth, PeriodicPhase.RESTART);
        }
        public PeriodicHealing(double amount, int intervalTicks) { this(amount, intervalTicks, -1, false); }
        public int initialDelayTicks() { return firstDelayTicks < 0 ? intervalTicks : firstDelayTicks; }
        public void execute(PeriodicExecution execution) {
            if (amount <= 0) return;
            if (onlyWhenMissingHealth) {
                var facts = execution.input().facts();
                if (facts.health() >= facts.maxHealth()) return;
            }
            execution.heal(amount);
        }
        public PeriodicHealing {
            Objects.requireNonNull(phase);
            DefinitionJson.range(amount, 0, 1000000, "amount"); DefinitionJson.range(intervalTicks, 1, 1200000, "interval_ticks");
            DefinitionJson.range(firstDelayTicks, -1, 1200000, "first_delay_ticks");
        }
    }
    public record InstantHealing(double amount) implements InstantBehavior {
        public void execute(InstantExecution execution) {
            var s = execution.source();
            execution.submit(new io.github.fatalitaya.combatnexus.combat.api.DomainCommand.Heal(
                new io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest(java.util.UUID.randomUUID(), s.target(),
                    new io.github.fatalitaya.combatnexus.combat.api.CombatSource(s.source(), s.owner(), java.util.Optional.empty()),
                    io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest.Reason.EFFECT, amount)));
        }
        public InstantHealing { DefinitionJson.range(amount, 0, 1000000, "amount"); }
    }
    public record InstantDamage(double amount, io.github.fatalitaya.combatnexus.api.id.NexusId damageType, java.util.Optional<java.util.UUID> direct) implements InstantBehavior {
        public InstantDamage { DefinitionJson.range(amount, 0, 1000000, "amount"); Objects.requireNonNull(damageType); Objects.requireNonNull(direct); }
        public void execute(InstantExecution execution) { execution.damage(amount, damageType, direct); }
    }

    public record PeriodicDamage(double amount, io.github.fatalitaya.combatnexus.api.id.NexusId damageType, int intervalTicks, float minimumHealth, int firstDelayTicks, PeriodicPhase phase) implements PeriodicBehavior {
        public PeriodicDamage(double amount, io.github.fatalitaya.combatnexus.api.id.NexusId damageType, int intervalTicks, float minimumHealth, int firstDelayTicks) {
            this(amount, damageType, intervalTicks, minimumHealth, firstDelayTicks, PeriodicPhase.RESTART);
        }
        public PeriodicDamage(double amount, io.github.fatalitaya.combatnexus.api.id.NexusId damageType, int intervalTicks) { this(amount, damageType, intervalTicks, 0, -1); }
        public PeriodicDamage(double amount, io.github.fatalitaya.combatnexus.api.id.NexusId damageType, int intervalTicks, float minimumHealth) { this(amount, damageType, intervalTicks, minimumHealth, -1); }
        public int initialDelayTicks() { return firstDelayTicks < 0 ? intervalTicks : firstDelayTicks; }
        public void execute(PeriodicExecution execution) {
            if (amount > 0 && (minimumHealth == 0 || execution.input().facts().health() > minimumHealth))
                execution.damage(amount, damageType, minimumHealth);
        }
        public PeriodicDamage {
            Objects.requireNonNull(phase);
            Objects.requireNonNull(damageType); DefinitionJson.range(amount, 0, 1000000, "amount");
            DefinitionJson.range(intervalTicks, 1, 1200000, "interval_ticks");
            DefinitionJson.range(firstDelayTicks, -1, 1200000, "first_delay_ticks");
            if (!Float.isFinite(minimumHealth) || minimumHealth < 0) throw new IllegalArgumentException("Invalid minimum health");
        }
    }
    public record HealingAdditive(double amount) implements NumericalBehavior {
        public void healing(io.github.fatalitaya.combatnexus.combat.healing.api.HealingPhase phase, io.github.fatalitaya.combatnexus.combat.healing.api.HealingCalculation calculation, BehaviorSource source) {
            if (phase == io.github.fatalitaya.combatnexus.combat.healing.api.HealingPhase.CONTRIBUTIONS && source.target().equals(calculation.request().target())) calculation.additive(source.source(), amount);
        }
        public HealingAdditive { DefinitionJson.range(amount, -1000000, 1000000, "amount"); }
    }
}
