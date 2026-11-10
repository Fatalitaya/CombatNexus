package io.github.fatalitaya.combatnexus.combat.healing.internal;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatListeners;
import io.github.fatalitaya.combatnexus.combat.healing.spi.HealingApplicationPlan;
import io.github.fatalitaya.combatnexus.combat.healing.spi.HealingApplicationPort;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatPublicationGate;

import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** World-owned application service using the owning server's admission, ancestry and delivery kernel. */
public final class DefaultHealingService implements HealingServiceControl {
    public HealingService businessView() {
        return new HealingService() {
            public HealingSettlement submit(HealingRequest request) { return DefaultHealingService.this.submit(request); }
            public HealingSettlement submit(HealingRequest request, io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent parent) { return DefaultHealingService.this.submit(request, parent); }
        };
    }
    private final CombatServerRuntime server;
    private final io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime world;
    private final HealingApplicationPort application;
    private final CombatListeners<HealingSettlement> listeners = new CombatListeners<>();
    private final CombatListeners<HealingSettlement> reactions = new CombatListeners<>();
    private boolean closed;

    public DefaultHealingService(CombatServerRuntime server, io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime world, HealingApplicationPort application) {
        this(server, world, application, listeners -> {});
    }
    public DefaultHealingService(CombatServerRuntime server, io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime world, HealingApplicationPort application, Consumer<CombatListeners<HealingSettlement>> configure) {
        this.server = Objects.requireNonNull(server, "server");
        this.world = Objects.requireNonNull(world);
        this.application = Objects.requireNonNull(application, "application");
        configure.accept(listeners); listeners.freeze();
        reactions.register(NexusId.fromNamespaceAndPath("combatnexus", "overflow_shield"), 0, callback -> io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.healing(world, callback));
        reactions.freeze();
    }
    @Override public HealingSettlement submit(HealingRequest request) { return submit(request, null); }
    @Override public HealingSettlement submit(HealingRequest request, CombatParent parent) {
        Objects.requireNonNull(request, "request");
        if (!server.claim(CombatOperation.HEALING, request.requestId())) return rejected(request, io.github.fatalitaya.combatnexus.combat.api.CommandRejection.Code.DUPLICATE, "Request already submitted");
        if (!server.isOwningThread()) return rejected(request, io.github.fatalitaya.combatnexus.combat.api.CommandRejection.Code.WRONG_THREAD, "Owning server thread required");
        server.requireThread();
        if (closed || server.world(world.worldSession()).orElse(null) != world) return rejected(request, io.github.fatalitaya.combatnexus.combat.api.CommandRejection.Code.CLOSED_SESSION, "World session closed");
        if (!io.github.fatalitaya.combatnexus.combat.driver.internal.ResolvedNumbers.isResolvedHealing(request)
                && !world.driverControl().allowsAutomatic(request.target(), io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope.NUMERICAL, request.source().id()))
            return rejected(request, io.github.fatalitaya.combatnexus.combat.api.CommandRejection.Code.INVALID_PHASE, "Numerical application belongs to an external driver");
        CombatLineage lineage;
        try { lineage = server.requestLineage(CombatOperation.HEALING, parent); }
        catch (IllegalStateException invalidScope) { return rejected(request, io.github.fatalitaya.combatnexus.combat.api.CommandRejection.Code.INVALID_PHASE, invalidScope.getMessage()); }
        var admission = server.admit(lineage);
        if (!admission.admitted()) return rejected(request, io.github.fatalitaya.combatnexus.combat.api.CommandRejection.Code.valueOf(admission.rejection().name()), "Admission limit");
        try (var completion = server.reserveCompletion(lineage)) {
            world.sourceInstallations().binding(request.source().owner().orElse(request.target()), request.source().id())
                    .ifPresent(server.ticket(lineage)::bindDefinitions);
            server.ticket(lineage).bindWorld(world.worldSession());
            HealingSettlement settlement = rejected(request);
            java.util.List<io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource> captured = java.util.List.of();
            java.util.List<HealingContribution> contributions = java.util.List.of();
            var beforeApplication = io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result.EMPTY;
            var identity = new io.github.fatalitaya.combatnexus.combat.api.CommandContext(request.requestId(), request.target(), CombatOperation.HEALING, java.util.Optional.of(request.source()),
                    java.util.Optional.of(new io.github.fatalitaya.combatnexus.combat.api.CommandContext.Identity(lineage.id(),lineage.rootId(),lineage.parentId(),lineage.depth())), io.github.fatalitaya.combatnexus.combat.internal.coordination.CombatActionScope.current(world.sessionId()));
            HealingApplicationPort.Attempt attempt = null;
            boolean applicationEntered = false;
            boolean applicationCompleted = false;
            try {
                try (var submission = server.ticket(lineage).enter(CombatExecutionScope.Phase.SUBMISSION)) {
                    HealingApplicationPlan plan;
                    try (var calculation = server.ticket(lineage).enter(CombatExecutionScope.Phase.COMPUTE)) {
                        long generation = server.ticket(lineage).definitions().generation();
                        attempt = application.open(request, generation);
                        var target = attempt.target().orElse(null);
                        if (target == null || target.world() != world.worldSession() || !target.identity().entity().equals(request.target())
                                || request.targetInstance().isPresent() && !request.targetInstance().get().equals(target.identity()))
                            return settlement;
                        server.ticket(lineage).bindSession(world.worldSession(), target.identity());
                        if (!target.alive()) {
                            settlement = new HealingSettlement(request, 0, 0, 0, HealingSettlement.Outcome.DEAD_TARGET,
                                    io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.EMPTY);
                            return settlement;
                        }
                        var frame = target.input().orElseThrow();
                        var roles = new java.util.EnumMap<io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs.Role, io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame>(io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs.Role.class);
                        var frames = new java.util.HashMap<java.util.UUID, io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame>();
                        frames.put(target.identity().entity(), frame);
                        roles.put(io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs.Role.TARGET, frame);
                        java.util.function.BiConsumer<io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs.Role, java.util.UUID> capture = (role, id) -> {
                            var capturedFrame = frames.get(id);
                            if (capturedFrame == null) capturedFrame = world.participantInput(id, generation).orElse(null);
                            if (capturedFrame != null) { frames.put(id, capturedFrame); roles.put(role, capturedFrame); }
                        };
                        request.healer().ifPresent(id -> capture.accept(io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs.Role.HEALER, id));
                        request.source().owner().ifPresent(id -> capture.accept(io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs.Role.EFFECT_OWNER, id));
                        captured = frame.behaviors();
                        plan = io.github.fatalitaya.combatnexus.combat.driver.internal.ResolvedNumbers.isResolvedHealing(request)
                                ? new io.github.fatalitaya.combatnexus.combat.healing.spi.HealingApplicationPlan(request, request.amount(), generation, java.util.Optional.of(frame), java.util.List.of(), io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.EMPTY)
                                : HealingPipeline.calculate(request, new io.github.fatalitaya.combatnexus.combat.api.input.ParticipantInputs(roles), server.ticket(lineage).definitions().domains().healingRules(),
                                io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions::calculate);
                        contributions = plan.contributions();
                    }
                    try (var applying = server.ticket(lineage).enter(CombatExecutionScope.Phase.APPLICATION)) {
                        applicationEntered = true;
                        beforeApplication = io.github.fatalitaya.combatnexus.combat.internal.transaction.BeforeApplicationExecutor.execute(world,plan.beforeApplication());
                        if (!beforeApplication.continueParent()) {
                            settlement = new HealingSettlement(request,plan.modifiedAmount(),0,0,beforeApplication.completed()?HealingSettlement.Outcome.CANCELLED:HealingSettlement.Outcome.UNKNOWN,
                                    new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal(java.util.List.of(),beforeApplication.completed()),captured,contributions).execution(beforeApplication,identity);
                            applicationCompleted = true;
                            return settlement;
                        }
                        var receipt = attempt.apply(plan.modifiedAmount());
                        var classified = classify(request, receipt);
                        settlement = new HealingSettlement(classified.request(), classified.modifiedAmount(), classified.actualRecovery(),
                            classified.overflow(), classified.outcome(), classified.journal(), captured, contributions, classified.rejection()).execution(beforeApplication,identity);
                        applicationCompleted = true;
                    }
                }
                return settlement;
            } finally {
                try {
                // A port exception may follow an unobserved native write. Never infer rollback or success.
                HealingSettlement frozen = applicationEntered && !applicationCompleted
                    ? new HealingSettlement(request, 0, 0, 0, HealingSettlement.Outcome.UNKNOWN, new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal(
                java.util.List.of(new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry(
                io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH, java.util.Optional.empty(), java.util.OptionalDouble.empty(),
                java.util.OptionalDouble.empty(), 0, io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Observation.UNKNOWN, false)), false)) : settlement;
                HealingSettlement delivered = new HealingSettlement(frozen.request(), frozen.modifiedAmount(), frozen.actualRecovery(),
                    frozen.overflow(), frozen.outcome(), frozen.journal(), captured, contributions, frozen.rejection()).execution(beforeApplication,identity);
                CombatPublicationGate gate = new CombatPublicationGate();
                io.github.fatalitaya.combatnexus.combat.spi.CombatBridgeAccess.healing(delivered,lineage);
                completion.publish(() -> {
                    if (gate.claim()) {
                        boolean complete = delivered.journal().applicationCompleted()
                            && (delivered.outcome() == HealingSettlement.Outcome.APPLIED || delivered.outcome() == HealingSettlement.Outcome.FULL_HEALTH);
                        reactions.dispatch(server.session(), lineage, delivered, complete ? EnumSet.allOf(CombatOperation.class) : Set.of());
                        io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.healingCompleted(server.session(), lineage, delivered);
                        listeners.dispatch(server.session(), lineage, delivered,
                            frozen.actualRecovery() > 0 ? EnumSet.allOf(CombatOperation.class) : Set.of());
                    }
                });
                } finally { if (attempt != null) attempt.close(); }
            }
        } finally {
            // Application/receipt failures never invent a successful settlement or undo committed work.
            server.finish(lineage);
            server.drain();
        }
    }
    private static HealingSettlement classify(HealingRequest request, HealingApplicationPort.Receipt receipt) {
        double accepted = receipt.acceptedAmount();
        double recovered = receipt.state() == HealingApplicationPort.State.WRITTEN
                ? Math.min(accepted, Math.max(0, receipt.healthAfter() - receipt.healthBefore())) : 0;
        double overflow = receipt.completed() && receipt.state() == HealingApplicationPort.State.WRITTEN
                ? Math.min(accepted - recovered, Math.max(0, accepted - Math.max(0, receipt.maximumHealth() - receipt.healthBefore()))) : 0;
        HealingSettlement.Outcome outcome = switch (receipt.state()) {
            case UNKNOWN_WRITE -> HealingSettlement.Outcome.UNKNOWN;
            case REJECTED -> HealingSettlement.Outcome.REJECTED;
            case DEAD_TARGET -> HealingSettlement.Outcome.DEAD_TARGET;
            case CANCELLED -> HealingSettlement.Outcome.CANCELLED;
            case ZERO_AMOUNT -> request.amount() == 0 ? HealingSettlement.Outcome.ZERO_INPUT : HealingSettlement.Outcome.ZERO_EFFECT;
            case WRITTEN -> !receipt.completed()
                    ? (recovered > 0 ? HealingSettlement.Outcome.PARTIAL : HealingSettlement.Outcome.UNKNOWN)
                    : (recovered > 0 ? HealingSettlement.Outcome.APPLIED
                        : receipt.healthBefore() >= receipt.maximumHealth() ? HealingSettlement.Outcome.FULL_HEALTH : HealingSettlement.Outcome.ZERO_EFFECT);
        };
        var journal = new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal(
                receipt.state() == HealingApplicationPort.State.UNKNOWN_WRITE ? java.util.List.of(
                    new io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry(
                        io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH, java.util.Optional.empty(),
                        java.util.OptionalDouble.of(receipt.healthBefore()), java.util.OptionalDouble.of(receipt.healthAfter()), 0,
                        io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Observation.UNKNOWN, false)) :
                receipt.state() == HealingApplicationPort.State.WRITTEN ? java.util.List.of(
                    io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Entry.write(
                        io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH,
                        receipt.healthBefore(), receipt.healthAfter(), recovered, receipt.completed())) : java.util.List.of(),
                receipt.completed());
        return new HealingSettlement(request, accepted, recovered, overflow, outcome, journal);
    }

    private static HealingSettlement rejected(HealingRequest request, io.github.fatalitaya.combatnexus.combat.api.CommandRejection.Code code, String detail) {
        return new HealingSettlement(request, 0, 0, 0, HealingSettlement.Outcome.REJECTED, io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.EMPTY,
            java.util.List.of(), java.util.List.of(), java.util.Optional.of(new io.github.fatalitaya.combatnexus.combat.api.CommandRejection(code, detail)));
    }
    private static HealingSettlement rejected(HealingRequest request) {
        return new HealingSettlement(request, 0, 0, 0, HealingSettlement.Outcome.REJECTED, io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.EMPTY);
    }
    @Override public void close() {
        server.requireThread();
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        listeners.clear(); reactions.clear(); closed = true;
    }
}
