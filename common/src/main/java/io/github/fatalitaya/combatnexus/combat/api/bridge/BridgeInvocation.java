package io.github.fatalitaya.combatnexus.combat.api.bridge;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.*;

/** Cold evidence, with no entity references or submission authority. */
public record BridgeInvocation(UUID invocation, UUID action, UUID target, NexusId capability, NexusId executor,
        Contribution nativeContribution, List<CommandEvidence> commands, List<DecisionEvidence> decisions, boolean closed, boolean returnedNormally) {
    public record CommandEvidence(Optional<CommandContext> context, ApplicationJournal journal, Optional<CommandRejection> rejection) {}
    public record DecisionEvidence(UUID invocation, NexusId policy, double input, double output, List<Float> samples, List<Double> multipliers, long generation, boolean completed) {
        public DecisionEvidence { samples=List.copyOf(samples); multipliers=List.copyOf(multipliers); }
    }
    public enum Contribution { PENDING, EXECUTED, SUPPRESSED, INSEPARABLE }
    public BridgeInvocation { commands=List.copyOf(commands); decisions=List.copyOf(decisions); }
    public boolean completed() { return closed && returnedNormally && nativeContribution!=Contribution.PENDING && commands.stream().allMatch(c->c.journal().applicationCompleted()) && decisions.stream().allMatch(DecisionEvidence::completed); }
}
