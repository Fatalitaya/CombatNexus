package probe;

import com.mojang.serialization.*;

import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult;
import io.github.fatalitaya.combatnexus.combat.api.scheduling.*;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldSettlement;
import io.github.fatalitaya.combatnexus.effect.api.*;

import net.minecraft.server.level.ServerLevel;

import java.util.*;

public final class SemanticExample {
    public record Combo(double amount) implements ScheduledIntent {
        public Combo {
            if (!Double.isFinite(amount) || amount < 0)
                throw new IllegalArgumentException("Invalid combo amount");
        }
    }

    public record Restricted() implements ScheduledIntent {}

    public record Nested() implements ScheduledIntent {}

    public record ThrowAfterWrite() implements ScheduledIntent {}

    public record Repeat() implements ScheduledIntent {}

    public record SettlementFact(long id, CombatOperation operation, boolean shieldPayload) {}

    private static final List<SettlementFact> SETTLEMENTS = new ArrayList<>();
    private static IntentExecution expired;
    private static PeriodicExecution expiredPeriodic;

    public static IntentExecution expired() {
        return expired;
    }

    public static PeriodicExecution expiredPeriodic() {
        return expiredPeriodic;
    }

    public static List<SettlementFact> settlements() {
        return List.copyOf(SETTLEMENTS);
    }

    public static void clear() {
        SETTLEMENTS.clear();
    }

    static void combo(IntentExecution execution, double amount) {
        expired = execution;
        execution.submit(
                new DomainCommand.Heal(
                        new HealingRequest(
                                UUID.randomUUID(),
                                execution.target(),
                                PlanExample.source("combo_child"),
                                HealingRequest.Reason.SKILL,
                                amount)));
        execution.submit(PlanExample.absorption(execution.target(), execution.source()));
    }

    public record Pulse() implements PeriodicBehavior {
        public int intervalTicks() {
            return 2;
        }

        public void execute(PeriodicExecution execution) {
            expiredPeriodic = execution;
            execution.execute(new Combo(2));
        }
    }

    public record OverflowPulse() implements PeriodicBehavior {
        public int intervalTicks() {
            return 2;
        }

        public void execute(PeriodicExecution execution) {
            execution.heal(2);
        }
    }

    public record FireProtection() implements ProtectionBehavior {
        public OptionalDouble protect(ProtectionContext context) {
            return context.proposed() <= 0
                            && context.inputs()
                                    .damageTags()
                                    .contains(
                                            io.github.fatalitaya.combatnexus.api.id.NexusId.parse(
                                                    "minecraft:is_fire"))
                    ? OptionalDouble.of(1)
                    : OptionalDouble.empty();
        }
    }

    public record OnSettlement(boolean shield) implements SettlementBehavior {
        public Set<CombatOperation> subscriptions() {
            return Set.of(shield ? CombatOperation.SHIELD : CombatOperation.ABSORPTION);
        }

        public Set<CombatOperation> permissions() {
            return Set.of(CombatOperation.HEALING);
        }

        public ReactionResult settle(SettlementContext context) {
            if (shield
                    && context.settlement()
                            .result(ShieldSettlement.class)
                            .filter(s -> !s.brokenSources().isEmpty())
                            .isEmpty()) return ReactionResult.completed();
            var behavior = context.source().orElseThrow();
            context.world()
                    .healing()
                    .submit(
                            new HealingRequest(
                                    UUID.randomUUID(),
                                    behavior.target(),
                                    PlanExample.source("settlement_heal"),
                                    HealingRequest.Reason.SKILL,
                                    shield ? 2 : 1),
                            context.authority().get().orElseThrow());
            return ReactionResult.completed();
        }
    }

    public static SourceResult install(
            ServerLevel level, UUID target, String kind, boolean persistent) {
        List<BehaviorComponent> components =
                switch (kind) {
                    case "pulse" -> List.of(new Pulse());
                    case "periodic_overflow" ->
                            List.of(
                                    new OverflowPulse(),
                                    new BuiltinBehaviors.OverflowShield(
                                            1,
                                            20,
                                            100,
                                            io.github.fatalitaya.combatnexus.combat.api.action
                                                    .TriggerLimit.ONCE));
                    case "fire" -> List.of(new FireProtection());
                    case "shield" -> List.of(new OnSettlement(true));
                    case "state" -> List.of(new OnSettlement(false));
                    default ->
                            throw new IllegalArgumentException("Unknown probe behavior: " + kind);
                };
        var effect = SdkExample.definition(new BehaviorPlan(0, components), List.of(), Set.of());
        return CombatAccess.find((WorldLookup) level)
                .orElseThrow()
                .sources()
                .install(
                        new SourceInstallation(
                                UUID.randomUUID(),
                                target,
                                new SourceInstance(
                                        UUID.randomUUID(),
                                        SdkExample.id("semantic_owner"),
                                        Optional.empty(),
                                        kind),
                                new SourcePlan(
                                        SdkExample.id(kind),
                                        Map.of(SdkExample.id(kind), effect),
                                        Map.of(),
                                        Map.of(),
                                        Map.of(),
                                        persistent)));
    }

    public static HealingSettlement run(CombatWorld world, UUID target, String mode) {
        return world.healing()
                .submit(
                        new HealingRequest(
                                UUID.randomUUID(),
                                target,
                                PlanExample.source(mode),
                                HealingRequest.Reason.SKILL,
                                1));
    }

    static void plan(HealingCalculation calculation, List<String> tags) {
        ScheduledIntent intent =
                switch (calculation.request().source().id().path()) {
                    case "combo" -> new Combo(2);
                    case "restricted" -> new Restricted();
                    case "nested" -> new Nested();
                    case "throw" -> new ThrowAfterWrite();
                    case "repeat" -> new Repeat();
                    default -> null;
                };
        if (intent != null)
            calculation.beforeApplication(
                    new BeforeApplicationPlan.Step(
                            new DomainCommand.Intent(
                                    UUID.randomUUID(),
                                    calculation.request().target(),
                                    calculation.request().source(),
                                    intent),
                            BeforeApplicationPlan.RejectionPolicy.CONTINUE));
    }

    static void register(CombatRegistrar api) {
        var both = Set.of(CombatOperation.HEALING, CombatOperation.ABSORPTION);
        api.intent(
                new IntentType<>(
                        SdkExample.id("combo"),
                        Combo.class,
                        Codec.DOUBLE.fieldOf("amount").xmap(Combo::new, Combo::amount),
                        both,
                        (value, execution) -> combo(execution, value.amount())));
        api.intent(
                new IntentType<>(
                        SdkExample.id("restricted"),
                        Restricted.class,
                        MapCodec.unit(new Restricted()),
                        Set.of(CombatOperation.HEALING),
                        (value, execution) ->
                                execution.submit(
                                        PlanExample.absorption(
                                                execution.target(), execution.source()))));
        api.intent(
                new IntentType<>(
                        SdkExample.id("nested"),
                        Nested.class,
                        MapCodec.unit(new Nested()),
                        Set.of(CombatOperation.HEALING, CombatOperation.INTENT),
                        (value, execution) ->
                                execution.submit(
                                        new DomainCommand.Intent(
                                                UUID.randomUUID(),
                                                execution.target(),
                                                execution.source(),
                                                new Combo(2)))));
        api.intent(
                new IntentType<>(
                        SdkExample.id("throw"),
                        ThrowAfterWrite.class,
                        MapCodec.unit(new ThrowAfterWrite()),
                        both,
                        (value, execution) -> {
                            execution.submit(
                                    PlanExample.absorption(execution.target(), execution.source()));
                            throw new IllegalStateException("Intentional failure after write");
                        }));
        api.intent(
                new IntentType<>(
                        SdkExample.id("repeat"),
                        Repeat.class,
                        MapCodec.unit(new Repeat()),
                        both,
                        (value, execution) -> {
                            for (int i = 0; i < 100; i++)
                                execution.submit(
                                        new DomainCommand.Heal(
                                                new HealingRequest(
                                                        UUID.randomUUID(),
                                                        execution.target(),
                                                        PlanExample.source("repeat_child"),
                                                        HealingRequest.Reason.SKILL,
                                                        0.01)));
                        }));
        api.behavior(
                new BehaviorType<>(
                        SdkExample.id("pulse"), Pulse.class, MapCodec.unit(new Pulse())));
        api.behavior(
                new BehaviorType<>(
                        SdkExample.id("overflow_pulse"),
                        OverflowPulse.class,
                        MapCodec.unit(new OverflowPulse())));
        api.behavior(
                new BehaviorType<>(
                        SdkExample.id("fire_protection"),
                        FireProtection.class,
                        MapCodec.unit(new FireProtection())));
        api.behavior(
                new BehaviorType<>(
                        SdkExample.id("on_settlement"),
                        OnSettlement.class,
                        Codec.BOOL
                                .fieldOf("shield")
                                .xmap(OnSettlement::new, OnSettlement::shield)));
        api.settlementPolicy(
                new SettlementPolicy(
                        SdkExample.id("settlement_log"),
                        0,
                        EnumSet.allOf(CombatOperation.class),
                        Set.of(),
                        context -> {
                            if (SETTLEMENTS.size() >= 4096) SETTLEMENTS.removeFirst();
                            SETTLEMENTS.add(
                                    new SettlementFact(
                                            context.settlement().identity().operation(),
                                            context.settlement().operation(),
                                            context.settlement().payload()
                                                    instanceof ShieldSettlement));
                            return ReactionResult.completed();
                        }));
    }
}
