package io.github.fatalitaya.combatnexus.combat.scheduling.internal;

import io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal;
import io.github.fatalitaya.combatnexus.combat.api.CommandRejection;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.scheduling.CombatScheduler;
import io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledTask;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions;
import io.github.fatalitaya.combatnexus.combat.internal.registration.IntentTypes;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.StateCommands;
import io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** World-owned domain commands; mutations enter through StateCommands. */
public final class WorldCombatScheduler implements CombatScheduler {
    private final CombatRuntime world;
    public WorldCombatScheduler(CombatRuntime world) { this.world = Objects.requireNonNull(world); }

    public List<ScheduledTask> snapshot(UUID target){var entity=world.level().getEntity(target);return entity instanceof NativeSchedules nativeTarget?nativeTarget.combatnexus$schedules().snapshot():List.of();}
    public CommandResult<ScheduledTask> schedule(UUID request,UUID target,ScheduledTask task){
        return StateCommands.submit(world,request,target,CombatOperation.STATE,Optional.of(task.source()),null,()->{
            for(var operation : IntentTypes.type(task.intent()).permissions()) BridgeExecutions.require(operation);
            if(!CombatExecutionScope.currentLineage(world.serverSession()).orElseThrow().permissions().containsAll(IntentTypes.type(task.intent()).permissions()))return CommandResult.rejected(CommandRejection.Code.INVALID_PHASE,"Scheduled operation exceeds parent permissions");
            ((NativeSchedules)world.level().getEntity(target)).combatnexus$schedules().add(task);
            return CommandResult.applied(task).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.STATE,task.source().id(),0,1,1,true)),true));
        });
    }
    public CommandResult<Boolean> cancel(UUID request,UUID target,UUID task){return StateCommands.submit(world,request,target,CombatOperation.STATE,null,()->CommandResult.applied(((NativeSchedules)world.level().getEntity(target)).combatnexus$schedules().remove(task)));}

}
