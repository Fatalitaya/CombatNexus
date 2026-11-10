package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatAction;
import java.util.*;

public record AbilitySnapshot(UUID execution, CombatAction action, AbilityGrant grant, AbilityTarget target,
                              int step, Status status, List<CommandEvidence> commands, OptionalLong nextTick) {
    public enum Status { RUNNING, WAITING, COMPLETED, REJECTED, CANCELLED, PARTIAL, UNKNOWN }
    /** Detached domain evidence, without retaining DamageRequest's native entities. */
    public record CommandEvidence(Optional<CommandContext> context, boolean applied,
                                  ApplicationJournal journal, Optional<CommandRejection> rejection) {
        public CommandEvidence { Objects.requireNonNull(context); Objects.requireNonNull(journal); Objects.requireNonNull(rejection); }
        public static CommandEvidence of(CommandOutcome<?> outcome) { return new CommandEvidence(outcome.context(),outcome.applied(),outcome.journal(),outcome.rejection()); }
    }
    public AbilitySnapshot {
        Objects.requireNonNull(execution); Objects.requireNonNull(action); Objects.requireNonNull(grant);
        Objects.requireNonNull(target); Objects.requireNonNull(status); commands=List.copyOf(commands); Objects.requireNonNull(nextTick);
        if (step<0 || step>64 || commands.size()>256) throw new IllegalArgumentException("Ability execution budget exceeded");
    }
}
