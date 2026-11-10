package io.github.fatalitaya.combatnexus.combat.api.scheduling;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
public interface CombatScheduler {
    CommandResult<ScheduledTask> schedule(UUID request,UUID target,ScheduledTask task);
    CommandResult<Boolean> cancel(UUID request,UUID target,UUID task);
    List<ScheduledTask> snapshot(UUID target);
}
