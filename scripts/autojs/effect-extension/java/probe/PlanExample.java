package probe;

import com.mojang.serialization.Codec;

import io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.input.FactKey;
import io.github.fatalitaya.combatnexus.combat.api.source.RestoredSource;
import io.github.fatalitaya.combatnexus.combat.damage.api.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.state.api.StateKey;

import java.util.*;
import java.util.function.UnaryOperator;

public final class PlanExample {
    public static final FactKey<List<String>> TAGS =
            new FactKey<>(SdkExample.id("tags"), Codec.STRING.listOf());
    public static final FactKey<String> CAPTURE_FAILURE =
            new FactKey<>(SdkExample.id("capture_failure"), Codec.STRING);
    public static final StateKey<Integer> RETAINED_STATE =
            new StateKey<>(
                    SdkExample.id("retained_state"),
                    Codec.INT,
                    UnaryOperator.identity(),
                    StateKey.Scope.ENTITY,
                    Set.of(),
                    true);
    public static final StateKey<Integer> DEATH_STATE =
            new StateKey<>(
                    SdkExample.id("death_state"),
                    Codec.INT,
                    UnaryOperator.identity(),
                    StateKey.Scope.ENTITY,
                    Set.of(StateKey.Boundary.DEATH),
                    false);
    private static CombatRegistrar registrar;
    private static final List<RestoredSource> RESTORED = new ArrayList<>();

    public static List<RestoredSource> restored() {
        return List.copyOf(RESTORED);
    }

    public static CombatRegistrar expiredRegistrar() {
        return registrar;
    }

    static CombatSource source(String path) {
        return new CombatSource(SdkExample.id(path), Optional.empty(), Optional.empty());
    }

    static DomainCommand.Absorption absorption(UUID target, CombatSource source) {
        return new DomainCommand.Absorption(
                new AbsorptionCommand(
                        UUID.randomUUID(),
                        target,
                        source,
                        AbsorptionCommand.Operation.GRANT,
                        2,
                        8));
    }

    static void register(CombatRegistrar api) {
        registrar = api;
        api.runtimeState(RETAINED_STATE);
        api.runtimeState(DEATH_STATE);
        api.restoredSource(SdkExample.id("restored_probe"), RESTORED::add);
        api.facts(
                TAGS,
                entity -> {
                    var tags = new ArrayList<>(entity.entityTags());
                    if (tags.contains("capture_write")) {
                        var result =
                                CombatAccess.find((WorldLookup) entity.level())
                                        .orElseThrow()
                                        .healing()
                                        .submit(
                                                new HealingRequest(
                                                        UUID.randomUUID(),
                                                        entity.getUUID(),
                                                        source("capture"),
                                                        HealingRequest.Reason.SKILL,
                                                        1));
                        tags.add("capture_rejection:" + result.rejection().orElseThrow().code());
                    }
                    return List.copyOf(tags);
                });
        api.facts(
                CAPTURE_FAILURE,
                entity -> {
                    if (entity.entityTags().contains("capture_fail"))
                        throw new IllegalStateException("Intentional capture failure");
                    return "captured";
                });
        api.healing()
                .processor(
                        SdkExample.id("healing_plan"),
                        HealingPhase.CONTRIBUTIONS,
                        10,
                        calculation -> {
                            var tags =
                                    calculation
                                            .input()
                                            .facts()
                                            .external()
                                            .find(TAGS)
                                            .orElse(List.of());
                            if (tags.contains("healing_plan"))
                                calculation.beforeApplication(
                                        new BeforeApplicationPlan.Step(
                                                absorption(
                                                        calculation.request().target(),
                                                        calculation.request().source()),
                                                BeforeApplicationPlan.RejectionPolicy.CONTINUE));
                            if (tags.contains("healing_plan_throw"))
                                throw new IllegalStateException(
                                        "Intentional healing plan rollback");
                            SemanticExample.plan(calculation, tags);
                        });
    }

    private static final Map<Long, BeforeApplicationPlan.Result> PLANS = new LinkedHashMap<>();

    public static BeforeApplicationPlan.Result before(long damage) {
        return Objects.requireNonNull(PLANS.get(damage));
    }

    static void registerDamage(DamageModuleRegisterEvent event) {
        event.registrar()
                .registerSettlementListener(
                        SdkExample.id("plan_evidence"),
                        0,
                        callback -> {
                            if (PLANS.size() >= 256) PLANS.clear();
                            PLANS.put(
                                    callback.snapshot().damageId(),
                                    callback.context().evidence().beforeApplication());
                        });
        event.registerPhaseProcessor(
                SdkExample.id("damage_plan"),
                new DamagePhaseProcessor() {
                    public DamagePhase phase() {
                        return DamagePhase.BASE_MODIFICATION;
                    }

                    public void apply(NativeDamageRuleContext context) {
                        var target = context.victim().getUUID();
                        var tags =
                                context.input(target)
                                        .facts()
                                        .external()
                                        .find(TAGS)
                                        .orElse(List.of());
                        if (tags.contains("plan_heal")) {
                            var command =
                                    new DomainCommand.Heal(
                                            new HealingRequest(
                                                    UUID.randomUUID(),
                                                    target,
                                                    source("plan"),
                                                    HealingRequest.Reason.SKILL,
                                                    4));
                            var policy =
                                    tags.contains("plan_abort")
                                            ? BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT
                                            : BeforeApplicationPlan.RejectionPolicy.CONTINUE;
                            context.beforeApplication(
                                    new BeforeApplicationPlan.Step(command, policy));
                            if (tags.contains("plan_twice"))
                                context.beforeApplication(
                                        new BeforeApplicationPlan.Step(
                                                new DomainCommand.Heal(
                                                        new HealingRequest(
                                                                UUID.randomUUID(),
                                                                target,
                                                                source("plan_second"),
                                                                HealingRequest.Reason.SKILL,
                                                                4)),
                                                policy));
                            if (tags.contains("plan_throw"))
                                throw new IllegalStateException("Intentional damage plan rollback");
                        }
                        if (tags.contains("semantic_combo"))
                            context.beforeApplication(
                                    new BeforeApplicationPlan.Step(
                                            new DomainCommand.Intent(
                                                    UUID.randomUUID(),
                                                    target,
                                                    source("semantic_damage"),
                                                    new SemanticExample.Combo(2)),
                                            BeforeApplicationPlan.RejectionPolicy.CONTINUE));
                    }
                });
    }
}
