package io.github.fatalitaya.combatnexus.combat.regeneration.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
public interface RegenerationCommands {
    List<RegenerationPlan> snapshot(UUID target);
    CommandResult<Boolean> replace(UUID request, RegenerationPlan plan);
    CommandResult<Boolean> replace(UUID request, RegenerationPlan plan, CombatParent parent);
    CommandResult<Boolean> remove(UUID request, UUID target, NexusId source);
}
