package io.github.fatalitaya.combatnexus.combat.healing.internal;
import io.github.fatalitaya.combatnexus.combat.healing.spi.HealingApplicationPlan;
import io.github.fatalitaya.combatnexus.combat.api.pipeline.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes;
import io.github.fatalitaya.combatnexus.formula.HealingFormula;
import java.util.*;
import java.util.function.Consumer;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Healing owns its numerical stages; transaction and native receipt classification stay in the service. */
public final class HealingPipeline {
    private static final System.Logger LOGGER = System.getLogger(HealingPipeline.class.getName());
    private static final DomainPipeline<HealingPhase, Consumer<HealingCalculation>> PIPELINE = create();
    private HealingPipeline() {}
    private static DomainPipeline<HealingPhase, Consumer<HealingCalculation>> create() {
        var builder = new DomainPipeline.Builder<HealingPhase, Consumer<HealingCalculation>>(HealingPhase.class);
        var sourceId = NexusId.fromNamespaceAndPath("combatnexus", "healing_power");
        builder.register(sourceId, HealingPhase.SOURCE, 0, false, ctx -> {
            var attribute = ctx.participants().find(io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs.Role.HEALER)
                    .map(frame -> frame.attributes().values().get(CombatAttributes.HEALING_POWER));
            ctx.amount(sourceId, HealingContribution.Kind.SOURCE, ctx.amount() * attribute.map(a -> a.effective()).orElse(1.0));
        });
        var id = NexusId.fromNamespaceAndPath("combatnexus", "healing_received");
        builder.register(id, HealingPhase.RECEIVED, 0, false, ctx -> {
            var attribute = ctx.input().attributes().values().get(CombatAttributes.HEALING_RECEIVED);
            ctx.amount(id, HealingContribution.Kind.RECEIVED, HealingFormula.modified(ctx.amount(), 1, attribute == null ? 1 : attribute.effective()));
        });
        return builder.freeze();
    }
    public static HealingApplicationPlan calculate(HealingRequest request, io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs participants,
            Map<NexusId, HealingRuleDefinition> rules,
            java.util.function.BiConsumer<HealingPhase, HealingCalculation> extensions) {
        var input = participants.target();
        var workspace = new HealingCalculation(request, participants);
        var ordered = rules.entrySet().stream().sorted(Comparator
                .<Map.Entry<NexusId, HealingRuleDefinition>>comparingInt(entry -> entry.getValue().priority()).reversed()
                .thenComparing(Map.Entry::getKey)).toList();
        for (var phase : HealingPhase.values()) {
            for (var participant : participants.roles().values().stream().distinct().toList())
                for (var source : participant.behaviors()) for (var component : source.plan().components())
                    if (component instanceof io.github.fatalitaya.combatnexus.combat.api.behavior.NumericalBehavior contribution) {
                        try { workspace.invokeContribution(ctx -> contribution.healing(phase, ctx, source)); }
                        catch (RuntimeException failure) { LOGGER.log(System.Logger.Level.DEBUG, "Healing source " + source.source() + " failed during " + phase, failure); }
                    }
            if (phase == HealingPhase.CONTRIBUTIONS || phase == HealingPhase.LIMITS) {
                workspace.invoke(ctx -> {
                    var matching = ordered.stream().filter(entry -> entry.getValue().phase() == phase && entry.getValue().matches(request, input)).toList();
                    if (phase == HealingPhase.LIMITS && matching.isEmpty()) return;
                    ctx.reduce(matching.stream().map(entry -> new HealingRuleContribution(
                            entry.getKey(), entry.getValue().additive(), entry.getValue().multiplier())).toList());
                });
            } else PIPELINE.execute(phase, e -> workspace.invoke(e.handler()), () -> false);
            extensions.accept(phase, workspace);
        }
        return new HealingApplicationPlan(request, workspace.amount(), input.definitionGeneration(), Optional.of(input), workspace.contributions(), workspace.beforeApplication());
    }
}
