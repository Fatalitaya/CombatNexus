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
        AbilityRequest request;
        final CombatAction action;
        List<ParticipantIdentity> targets;
        boolean restored;
        final DriverScope scope;
        AbilityDefinition definition;
        List<Cooldown> cooldownSlots=List.of();
        boolean cooldownFinished;
        boolean released;
        boolean terminalPublished;
        final List<AbilitySnapshot.CommandEvidence> commands = new ArrayList<>();
        AbilityParameters state = AbilityParameters.EMPTY;
        int step;
        AbilityExecution.Invocation invocation=AbilityExecution.Invocation.FIRST;
        AbilitySnapshot.Waiting waiting=AbilitySnapshot.Waiting.NONE;
        int remainingInputs;
        Optional<AbilitySnapshot.CloseReason> closeReason=Optional.empty();
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
            waiting=AbilitySnapshot.Waiting.DELAY;remainingInputs=0;
            state = continuation.state();
            next = OptionalLong.of(Math.addExact(now, continuation.delayTicks()));
            status = AbilitySnapshot.Status.WAITING;
        }

        void awaitInput(AbilityStepResult.AwaitInput window,long now) {
            if(step>=63)throw new IllegalStateException("Ability step capacity exceeded");
            if(invocation!=AbilityExecution.Invocation.INPUT){
                remainingInputs=window.attempts();next=OptionalLong.of(Math.addExact(now,window.ticks()));
            }else remainingInputs=Math.min(remainingInputs,window.attempts());
            state=window.state();waiting=AbilitySnapshot.Waiting.INPUT;status=AbilitySnapshot.Status.WAITING;
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
            status = terminal;waiting=AbilitySnapshot.Waiting.NONE;remainingInputs=0;
            next = OptionalLong.empty();
        }

        boolean hasWrites() {
            return commands.stream().anyMatch(e -> !e.journal().entries().isEmpty());
        }

        void failed() {
            terminate(
                    (hasWrites()||released) ? AbilitySnapshot.Status.PARTIAL : AbilitySnapshot.Status.UNKNOWN);
        }

        void refused() {
            terminate(
                    (hasWrites()||released) ? AbilitySnapshot.Status.PARTIAL : AbilitySnapshot.Status.REJECTED);
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
            execution.restored = true;execution.released=entry.released();
            execution.state = entry.state();
            execution.step = entry.step();
            execution.commands.addAll(entry.commands());
            execution.status = AbilitySnapshot.Status.WAITING;execution.waiting=AbilitySnapshot.Waiting.DELAY;
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
                    samples,released);
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
                    next,waiting,remainingInputs,closeReason);
        }
    }

    static Key key(AbilityGrant grant) {
        return new Key(grant.actor().entity(), grant.source().instance(), grant.contribution());
    }

    static AbilityCooldownSlot independent(AbilityGrant grant,AbilityDefinition definition) {
        return definition.cooldownOwner()==AbilityDefinition.CooldownOwner.PLAYER_SKILL
            ? new AbilityCooldownSlot.Skill(grant.ability())
            : new AbilityCooldownSlot.Contribution(grant.source().instance(),grant.contribution());
    }
    static String slotKey(AbilityCooldownSlot slot) {
        if(slot instanceof AbilityCooldownSlot.Skill skill)return "skill/"+skill.ability();
        if(slot instanceof AbilityCooldownSlot.Shared shared)return "shared/"+shared.group();
        var contribution=(AbilityCooldownSlot.Contribution)slot;
        return "source/"+contribution.source()+"/"+contribution.contribution();
    }
    static Cooldown cooldown(UUID actor,AbilityCooldownSlot slot,NexusId sourceClock) {
        String key=slotKey(slot);
        NexusId clock=sourceClock;
        if(slot instanceof AbilityCooldownSlot.Skill skill)clock=new NexusId("combatnexus","ability_cooldown/skill/"+skill.ability().namespace()+"/"+skill.ability().path());
        if(slot instanceof AbilityCooldownSlot.Shared shared)clock=new NexusId("combatnexus","ability_cooldown/shared/"+shared.group().namespace()+"/"+shared.group().path());
        return new Cooldown(actor,key,clock);
    }
    static List<Cooldown> cooldowns(AbilityGrant grant,AbilityDefinition definition,NexusId sourceClock) {
        var result=new ArrayList<Cooldown>();
        result.add(cooldown(grant.actor().entity(),independent(grant,definition),sourceClock));
        definition.sharedCooldown().ifPresent(group->result.add(cooldown(grant.actor().entity(),new AbilityCooldownSlot.Shared(group),sourceClock)));
        return List.copyOf(result);
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
                                                        - clock.applyAsLong(e.getKey().clock())),clearOnDeath.contains(e.getKey())))
                .filter(e -> e.remaining() > 0)
                .toList();
    }

    void restoreCooldown(UUID actor, SavedAbilityCooldown saved, long now) {
        if(saved.clearOnDeath())clearOnDeath.add(new Cooldown(actor,saved.key(),saved.clock()));
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
        clearOnDeath.retainAll(cooldowns.keySet());
    }

    final Map<Key, AbilityGrant> grants = new LinkedHashMap<>();
    final Map<Cooldown, Long> cooldowns = new HashMap<>();
    final Set<Cooldown> clearOnDeath = new HashSet<>();
    final Map<UUID, Execution> executions = new LinkedHashMap<>();
}
