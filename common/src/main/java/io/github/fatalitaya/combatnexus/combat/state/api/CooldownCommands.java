package io.github.fatalitaya.combatnexus.combat.state.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
public interface CooldownCommands {
    Optional<CooldownStamp> snapshot(UUID target, NexusId source);
    long remaining(UUID target, NexusId source, int duration);
    CommandResult<Boolean> submit(CooldownCommand command);
    CommandResult<Boolean> submit(CooldownCommand command, CombatParent parent);
}
