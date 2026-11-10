package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.scheduling.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.*;
import io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules;
/** Called only by the native entity tick. Uses the shared kernel and completion FIFO. */
public final class ScheduledOperations {
    private ScheduledOperations() {}
    public static void tick(LivingEntity entity) {
        if(!entity.isAlive()||!(entity.level() instanceof ServerLevel level))return;
        var kernel=CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (level.getServer()));if(kernel.isEmpty())return;
        var world=kernel.get().world(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).combatnexus$worldSession());if(world.isEmpty())return;
        var store=((NativeSchedules)entity).combatnexus$schedules();
        for(var task:store.snapshot()) {
            if(!world.get().timeControl().advancing(entity.getUUID(),task.source().id()) || task.nextTick()>world.get().businessTick(entity.getUUID(), task.source().id()))continue;
            if(!world.get().timeControl().mayExecute())break;
            if(task.intent() instanceof io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityContinuationIntent continuation){
                if(world.get().abilityControl().mayResume(continuation.execution()))world.get().abilityControl().resume(entity.getUUID(),continuation.execution(),continuation.step());
                continue;
            }
            StateCommands.submit(world.get(),UUID.randomUUID(),entity.getUUID(),CombatOperation.STATE,Optional.of(task.source()),null,()->{
                if(!store.remove(task.id()))return CommandResult.rejected(CommandRejection.Code.NOT_FOUND,"Schedule already consumed");

                var result=BeforeApplicationExecutor.execute(world.get(),new BeforeApplicationPlan(List.of(new BeforeApplicationPlan.Step(new DomainCommand.Intent(UUID.randomUUID(),entity.getUUID(),task.source(),task.intent()),BeforeApplicationPlan.RejectionPolicy.CONTINUE))));
                if(result.completed()&&task.repetitions()>1&&entity.isAlive())store.add(new ScheduledTask(task.id(),task.source(),task.installation(),Math.addExact(world.get().businessTick(entity.getUUID(), task.source().id()),task.interval()),task.interval(),task.repetitions()-1,task.intent(),task.clearOn(),task.persistent()));
                return CommandResult.applied(result).withJournal(new ApplicationJournal(result.steps().stream().flatMap(s->s.journal().entries().stream()).toList(),result.completed()));
            });
        }
    }
}
