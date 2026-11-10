package io.github.fatalitaya.combatnexus.combat.ability.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.ability.api.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatAction;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope;

import java.util.*;

/** Pure execution values; platform services retain authority and side effects. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class AbilityExecutionState {
    record Key(UUID actor, UUID source, NexusId contribution) {}

    record Cooldown(UUID actor, String key, NexusId clock) {}

    static final class Execution {
        final AbilityRequest request;
        final CombatAction action;
        List<ParticipantIdentity> targets;
        boolean restored;
        final DriverScope scope;
        final List<AbilitySnapshot.CommandEvidence> commands = new ArrayList<>();
        AbilityParameters state = AbilityParameters.EMPTY;
        int step;
        AbilitySnapshot.Status status = AbilitySnapshot.Status.RUNNING;
        OptionalLong next = OptionalLong.empty();

        Execution(
                AbilityRequest request,
                CombatAction action,
                List<ParticipantIdentity> targets,
                DriverScope scope) {
            this.request = request;
            this.action = action;
            this.targets = List.copyOf(targets);
            this.scope = scope;
        }

        void waiting(AbilityContinuation continuation, long now) {
            if (step >= 63)
                throw new IllegalStateException("Ability continuation capacity exceeded");
            state = continuation.state();
            next = OptionalLong.of(Math.addExact(now, continuation.delayTicks()));
            status = AbilitySnapshot.Status.WAITING;
        }

        boolean mayResume(UUID actor, int nextStep) {
            return request.grant().actor().entity().equals(actor)
                    && status == AbilitySnapshot.Status.WAITING
                    && step + 1 == nextStep;
        }

        void resume(int nextStep) {
            step = nextStep;
            next = OptionalLong.empty();
            status = AbilitySnapshot.Status.RUNNING;
        }

        void terminate(AbilitySnapshot.Status terminal) {
            status = terminal;
            next = OptionalLong.empty();
        }

        boolean hasWrites() {
            return commands.stream().anyMatch(e -> !e.journal().entries().isEmpty());
        }

        void failed() {
            terminate(
                    hasWrites() ? AbilitySnapshot.Status.PARTIAL : AbilitySnapshot.Status.UNKNOWN);
        }

        void refused() {
            terminate(
                    hasWrites() ? AbilitySnapshot.Status.PARTIAL : AbilitySnapshot.Status.REJECTED);
        }

        boolean record(CommandOutcome<?> result) {
            commands.add(AbilitySnapshot.CommandEvidence.of(result));
            return result.completed();
        }

        ApplicationJournal journal(int firstCommand, boolean complete) {
            return new ApplicationJournal(
                    commands.subList(firstCommand, commands.size()).stream()
                            .flatMap(e -> e.journal().entries().stream())
                            .toList(),
                    complete
                            && commands.stream().allMatch(e -> e.journal().applicationCompleted()));
        }

        static Execution restore(AbilityGrant grant, UUID world, SavedAbility entry, long now) {
            var request =
                    new AbilityRequest(entry.execution(), grant, entry.target(), entry.input());
            var execution =
                    new Execution(
                            request,
                            new CombatAction(entry.action(), world, grant.actor().entity()),
                            List.of(),
                            entry.scope());
            execution.restored = true;
            execution.state = entry.state();
            execution.step = entry.step();
            execution.commands.addAll(entry.commands());
            execution.status = AbilitySnapshot.Status.WAITING;
            execution.next = OptionalLong.of(Math.addExact(now, entry.remaining()));
            return execution;
        }

        SavedAbility save(
                AbilityDefinition definition,
                long now,
                List<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample> samples) {
            return new SavedAbility(
                    request.requestId(),
                    action.id(),
                    request.grant().source().instance(),
                    request.grant().contribution(),
                    definition,
                    request.target(),
                    request.input(),
                    state,
                    step,
                    scope,
                    Math.max(0, next.orElseThrow() - now),
                    commands,
                    samples);
        }

        AbilitySnapshot snapshot() {
            return new AbilitySnapshot(
                    request.requestId(),
                    action,
                    request.grant(),
                    request.target(),
                    step,
                    status,
                    commands,
                    next);
        }
    }

    static Key key(AbilityGrant grant) {
        return new Key(grant.actor().entity(), grant.source().instance(), grant.contribution());
    }

    static Cooldown cooldown(AbilityGrant grant, AbilityDefinition definition, NexusId sourceKey) {
        return definition
                .sharedCooldown()
                .map(
                        group ->
                                new Cooldown(
                                        grant.actor().entity(),
                                        "shared/" + group,
                                        new NexusId(
                                                "combatnexus",
                                                "ability_cooldown/"
                                                        + group.namespace()
                                                        + "/"
                                                        + group.path())))
                .orElseGet(
                        () ->
                                new Cooldown(
                                        grant.actor().entity(),
                                        grant.source().instance() + "/" + grant.contribution(),
                                        sourceKey));
    }

    long remaining(Cooldown key, long now) {
        return Math.max(0, cooldowns.getOrDefault(key, 0L) - now);
    }

    void grant(AbilityGrant grant) {
        if (grants.size() >= 4096)
            throw new IllegalStateException("Ability grant capacity exceeded");
        grants.put(key(grant), grant);
    }

    static long cooldownDeadline(long now, long ticks) {
        return Math.addExact(now, ticks);
    }

    List<SavedAbilityCooldown> saveCooldowns(
            UUID actor, java.util.function.ToLongFunction<NexusId> clock) {
        return cooldowns.entrySet().stream()
                .filter(e -> e.getKey().actor().equals(actor))
                .map(
                        e ->
                                new SavedAbilityCooldown(
                                        e.getKey().key(),
                                        e.getKey().clock(),
                                        Math.max(
                                                0,
                                                e.getValue()
                                                        - clock.applyAsLong(e.getKey().clock()))))
                .filter(e -> e.remaining() > 0)
                .toList();
    }

    void restoreCooldown(UUID actor, SavedAbilityCooldown saved, long now) {
        cooldowns.put(
                new Cooldown(actor, saved.key(), saved.clock()),
                cooldownDeadline(now, saved.remaining()));
    }

    void trim(java.util.function.ToLongFunction<Cooldown> clock) {
        var iterator = executions.values().iterator();
        while (executions.size() >= 256 && iterator.hasNext()) {
            var execution = iterator.next();
            if (execution.status != AbilitySnapshot.Status.WAITING
                    && execution.status != AbilitySnapshot.Status.RUNNING) iterator.remove();
        }
        cooldowns.entrySet().removeIf(e -> e.getValue() <= clock.applyAsLong(e.getKey()));
    }

    final Map<Key, AbilityGrant> grants = new LinkedHashMap<>();
    final Map<Cooldown, Long> cooldowns = new HashMap<>();
    final Map<UUID, Execution> executions = new LinkedHashMap<>();
}
