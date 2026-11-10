package io.github.fatalitaya.combatnexus.combat.internal.transaction;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;

import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import java.util.*;
import java.util.function.Supplier;

/** Kernel admission for non-health state commands. No public service can issue application authority. */
public final class StateCommands {
    private StateCommands() {}
    public static <T> CommandResult<T> submit(CombatRuntime world, UUID request, UUID target,
            CombatOperation operation, CombatParent parent, Supplier<CommandResult<T>> action) {
        return submit(world, request, target, operation, Optional.empty(), parent, action);
    }
    public static <T> CommandResult<T> submit(CombatRuntime world, UUID request, UUID target,
            CombatOperation operation, Optional<CombatSource> source, CombatParent parent, Supplier<CommandResult<T>> action) {
        return submit(world, request, target, operation, source, parent, null, action);
    }
    public static <T> CommandResult<T> submit(CombatRuntime world, UUID request, UUID target,
            CombatOperation operation, Optional<CombatSource> source, CombatParent parent,
            io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration definitions, Supplier<CommandResult<T>> action) {
        return submit(world, request, target, operation, source, parent, definitions, false, action);
    }
    public static <T> CommandResult<T> submit(CombatRuntime world, UUID request, UUID target,
            CombatOperation operation, Optional<CombatSource> source, CombatParent parent,
            io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration definitions,
            boolean allowDyingTarget, Supplier<CommandResult<T>> action) {
        var context = new CommandContext(request, target, operation, source, Optional.empty(), Optional.empty());
        var result = execute(world, request, target, operation, source, parent, definitions, allowDyingTarget, action);
        result=result.context().isPresent() ? result : result.withContext(context);
        io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions.record(result);
        return result;
    }
    private static <T> CommandResult<T> execute(CombatRuntime world, UUID request, UUID target,
            CombatOperation operation, Optional<CombatSource> source, CombatParent parent,
            io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration definitions, boolean allowDyingTarget, Supplier<CommandResult<T>> action) {
        var server = world.serverRuntime();
        if (server.isClosed() || world.commandSessionClosed()) return CommandResult.rejected(CommandRejection.Code.CLOSED_SESSION, "World session unavailable");
        if (!server.claim(operation, request)) return CommandResult.rejected(CommandRejection.Code.DUPLICATE, "Request already submitted");
        if (!server.isOwningThread()) return CommandResult.rejected(CommandRejection.Code.WRONG_THREAD, "Owning server thread required");
        if (server.world(world.worldSession()).orElse(null) != world) return CommandResult.rejected(CommandRejection.Code.CLOSED_SESSION, "World session unavailable");
        if (!world.targetAlive(target) && !(allowDyingTarget && operation == CombatOperation.MOVEMENT
                && world.level().getEntity(target) instanceof net.minecraft.world.entity.LivingEntity entity && !entity.isRemoved())) return CommandResult.rejected(CommandRejection.Code.TARGET_UNAVAILABLE, "Live target required");
        CombatLineage lineage;
        try { lineage = server.requestLineage(operation, parent); }
        catch (IllegalStateException rejected) { return CommandResult.rejected(CommandRejection.Code.INVALID_PHASE, rejected.getMessage()); }
        var admission = server.admit(lineage);
        if (!admission.admitted()) return CommandResult.rejected(CommandRejection.Code.valueOf(admission.rejection().name()), "Admission limit");
        try (var completion = server.reserveCompletion(lineage)) {
            var ticket = server.ticket(lineage);
            var binding = definitions != null ? Optional.of(definitions)
                : source.flatMap(value -> world.sourceInstallations().binding(value.owner().orElse(target), value.id()));
            binding.ifPresent(ticket::bindDefinitions);
            ticket.bindTarget(world, target);
            var captured = world.behaviors().capture(target);
            CommandResult<T> result;
            try (var scope = ticket.enter(CombatExecutionScope.Phase.APPLICATION);
                    var stateScope = StateApplicationScope.enter(ticket)) {
                try { result = action.get(); }
                catch (RuntimeException failure) {
                    result = CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "State application incomplete; inspect owner receipt");
                }
            }
            result = result.withContext(new CommandContext(request, target, operation, source,
                    Optional.of(new CommandContext.Identity(lineage.id(), lineage.rootId(), lineage.parentId(), lineage.depth())), io.github.fatalitaya.combatnexus.combat.internal.coordination.CombatActionScope.current(world.sessionId())));
            var delivered = result;
            completion.publish(() -> {
                var envelope=new io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementEnvelope(operation,
                    delivered.context().orElseThrow().identity().orElseThrow(),target,source,delivered.journal(),delivered);
                io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.completed(world,lineage,envelope,captured);
                io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.stateCompleted(server.session(), lineage, delivered);
            });
            return result;
        } finally { server.finish(lineage); server.drain(); }
    }
}
