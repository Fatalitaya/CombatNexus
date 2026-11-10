package io.github.fatalitaya.combatnexus.combat.ability.internal;

import static io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityExecutionState.*;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.ability.api.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatAction;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.coordination.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.*;
import io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope;
import io.github.fatalitaya.combatnexus.effect.api.EffectSnapshot.RestrictedAction;
import java.util.*;

/** Grant ownership and execution state; admission, domain writes and delivery stay in the shared kernel. */
public final class DefaultAbilityService implements AbilityService {
    private final AbilityExecutionState storage=new AbilityExecutionState();
    private final CombatRuntime world;
    public DefaultAbilityService(CombatRuntime world){this.world=Objects.requireNonNull(world);}
    public AbilityService businessView(){return new AbilityService(){
        public Map<NexusId,AbilityDefinition> definitions(){return DefaultAbilityService.this.definitions();}
        public AbilityDefinition definition(AbilityGrant grant) { return DefaultAbilityService.this.definition(grant); }
        public List<AbilityGrant> grants(UUID actor){return DefaultAbilityService.this.grants(actor);}
        public AbilityAvailability evaluate(AbilityRequest request){return DefaultAbilityService.this.evaluate(request);}
        public AbilityResult execute(AbilityRequest request){return DefaultAbilityService.this.execute(request);}
        public AbilityResult execute(AbilityRequest request,CombatParent parent){return DefaultAbilityService.this.execute(request,parent);}
        public AbilityResult reinput(AbilityReinput request){return DefaultAbilityService.this.reinput(request);}
        public AbilityResult reinput(AbilityReinput request,CombatParent parent){return DefaultAbilityService.this.reinput(request,parent);}
        public CommandResult<Boolean> cancel(UUID request,UUID actor,UUID execution){return DefaultAbilityService.this.cancel(request,actor,execution);}
        public AbilityCooldownSnapshot cooldown(UUID actor,AbilityCooldownSlot slot){return DefaultAbilityService.this.cooldown(actor,slot);}
        public CommandResult<AbilityCooldownSnapshot.Change> cooldown(AbilityCooldownCommand command){return DefaultAbilityService.this.cooldown(command);}
        public CommandResult<AbilityCooldownSnapshot.Change> cooldown(AbilityCooldownCommand command,CombatParent parent){return DefaultAbilityService.this.cooldown(command,parent);}
        public Optional<AbilitySnapshot> execution(UUID id){return DefaultAbilityService.this.execution(id);}
    };}
    public AbilityDefinition definition(AbilityGrant grant) {
        world.requireActive();
        return world.sourceInstallations().definitions(grant.actor().entity(), grant.source()).domains().abilities().get(grant.ability());
    }
    public Map<NexusId,AbilityDefinition> definitions(){world.requireActive();return world.domainDefinitions().abilities();}
    public List<AbilityGrant> grants(UUID actor){world.requireActive();return storage.grants.values().stream().filter(g->g.actor().entity().equals(actor)&&current(g)).toList();}
    private static NexusId sourceKey(AbilityGrant grant){return SourceInstallations.key(grant.source());}
    private boolean current(AbilityGrant grant){
        return grant.equals(storage.grants.get(key(grant)))&&grant.world().equals(world.sessionId())
            &&grant.generation()==world.sourceInstallations().definitions(grant.actor().entity(), grant.source()).generation()
            &&world.participantIdentity(grant.actor().entity()).filter(grant.actor()::equals).isPresent()
            &&world.sourceInstallations().active(grant.actor().entity(),grant.source().instance());
    }
    /** Called only by the source installation ledger after the base contribution writes. */
    public void reconcile(UUID actor,SourceInstance source,SourcePlan plan){
        world.requireActive();CombatMutationGuard.requireWorldWritePhase();
        var identity=world.participantIdentity(actor).orElseThrow();
        var definitions = world.sourceInstallations().definitions(actor, source);
        for(var existing:List.copyOf(storage.grants.values()))if(existing.actor().entity().equals(actor)&&existing.source().instance().equals(source.instance())
                &&(!Objects.equals(plan.abilities().get(existing.contribution()),existing.ability())||existing.generation()!=definitions.generation()))remove(existing);
        for(var entry:plan.abilities().entrySet()){
            var definition=definitions.domains().abilities().get(entry.getValue());
            if(definition==null)throw new IllegalArgumentException("Unknown granted ability: "+entry.getValue());
            var key=new Key(actor,source.instance(),entry.getKey());
            if(!storage.grants.containsKey(key)){
                storage.grant(new AbilityGrant(UUID.randomUUID(),world.sessionId(),identity,source,entry.getKey(),entry.getValue(),definitions.generation()));
            }
        }
    }
    public void sourceRemoved(UUID actor,SourceInstance source){
        world.requireActive();CombatMutationGuard.requireWorldWritePhase();
        for(var grant:List.copyOf(storage.grants.values()))if(grant.actor().entity().equals(actor)&&grant.source().instance().equals(source.instance()))remove(grant);
    }
    private void remove(AbilityGrant grant){
        storage.grants.remove(key(grant));
        for(var execution:storage.executions.values())if(execution.request.grant().id().equals(grant.id())&&execution.status==AbilitySnapshot.Status.WAITING)cancelPending(execution,AbilitySnapshot.CloseReason.SOURCE_REMOVED);
    }
    private List<Cooldown> slots(AbilityGrant grant,AbilityDefinition definition){return AbilityExecutionState.cooldowns(grant,definition,sourceKey(grant));}
    private Cooldown slot(UUID actor,AbilityCooldownSlot slot){
        String key=slotKey(slot);
        for(var existing:storage.cooldowns.keySet())if(existing.actor().equals(actor)&&existing.key().equals(key))return existing;
        NexusId clock=new NexusId("combatnexus","ability_cooldown");
        if(slot instanceof AbilityCooldownSlot.Contribution contribution){
            var grant=storage.grants.get(new Key(actor,contribution.source(),contribution.contribution()));
            if(grant==null)throw new IllegalArgumentException("Contribution cooldown source absent");
            clock=sourceKey(grant);
        }
        return AbilityExecutionState.cooldown(actor,slot,clock);
    }
    private Optional<UUID> occupying(Cooldown slot){return storage.executions.values().stream()
        .filter(e->(e.status==AbilitySnapshot.Status.RUNNING||e.status==AbilitySnapshot.Status.WAITING)&&e.cooldownSlots.contains(slot))
        .map(e->e.request.requestId()).findFirst();}
    private long remaining(AbilityGrant grant,AbilityDefinition definition){return slots(grant,definition).stream()
        .mapToLong(key->storage.remaining(key,world.businessTick(key.actor(),key.clock()))).max().orElse(0);}
    public AbilityCooldownSnapshot cooldown(UUID actor,AbilityCooldownSlot selected){
        world.requireActive();var key=slot(actor,selected);
        return new AbilityCooldownSnapshot(actor,selected,storage.remaining(key,world.businessTick(actor,key.clock())),occupying(key));
    }
    public CommandResult<AbilityCooldownSnapshot.Change> cooldown(AbilityCooldownCommand command){return cooldown(command,null);}
    public CommandResult<AbilityCooldownSnapshot.Change> cooldown(AbilityCooldownCommand command,CombatParent parent){
        return StateCommands.submit(world,command.requestId(),command.actor(),CombatOperation.ABILITY,Optional.of(command.source()),parent,()->{
            var key=slot(command.actor(),command.slot());
            var before=cooldown(command.actor(),command.slot());
            long now=world.businessTick(key.actor(),key.clock());
            if(command.operation()!=AbilityCooldownCommand.Operation.CLEAR&&!storage.cooldowns.containsKey(key)&&storage.cooldowns.size()>=4096)
                return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Ability cooldown capacity exceeded");
            switch(command.operation()){
                case CLEAR -> storage.cooldowns.remove(key);
                case START -> {if(before.remaining()==0)storage.cooldowns.put(key,cooldownDeadline(now,command.ticks()));}
                case RESET -> storage.cooldowns.put(key,cooldownDeadline(now,command.ticks()));
            }
            var after=cooldown(command.actor(),command.slot());
            return CommandResult.applied(new AbilityCooldownSnapshot.Change(before,after)).withJournal(new ApplicationJournal(List.of(
                ApplicationJournal.Entry.write(ApplicationJournal.Resource.STATE,command.source().id(),before.remaining(),after.remaining(),Math.abs(after.remaining()-before.remaining()),true)),true));
        });
    }
    private void startCooldowns(Execution execution){
        var definition=execution.definition;
        for(int i=0;i<execution.cooldownSlots.size();i++){
            var key=execution.cooldownSlots.get(i);int ticks=i==0?definition.cooldownTicks():definition.sharedCooldownTicks();
            storage.cooldowns.merge(key,cooldownDeadline(world.businessTick(key.actor(),key.clock()),ticks),Math::max);
            if(definition.deathCooldown()==AbilityDefinition.DeathCooldown.CLEAR)storage.clearOnDeath.add(key);
            else storage.clearOnDeath.remove(key);
        }
    }
    private void fact(Execution execution,AbilityFact.Kind kind){
        var grant=execution.request.grant();
        var fact=new AbilityFact(kind,grant.actor(),grant.ability(),grant.source(),execution.request.requestId(),execution.action,
            world.businessTick(grant.actor().entity(),sourceKey(grant)),execution.status,execution.released);
        var server=world.serverRuntime();var lineage=CombatExecutionScope.currentLineage(world.serverSession()).orElse(null);
        var slot=lineage!=null&&lineage.operation()==CombatOperation.ABILITY?server.reserveCompletion(lineage):server.rejectionCompletion().orElseThrow();
        try(slot){slot.publish(()->{
            if(lineage!=null){io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.abilityCompleted(server.session(),lineage,fact);return;}
            var notification=CombatLineage.root(CombatOperation.ABILITY);
            if(!server.admit(notification).admitted()){io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.abilityCompleted(server.session(),null,fact);return;}
            try{io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.abilityCompleted(server.session(),notification,fact);}
            finally{server.finish(notification);}
        });}
    }
    private void terminal(Execution execution){
        finishCooldowns(execution);
        if(!execution.terminalPublished){execution.terminalPublished=true;fact(execution,AbilityFact.Kind.TERMINAL);}
    }
    private void finishCooldowns(Execution execution){
        if(execution.cooldownFinished)return;
        execution.cooldownFinished=true;
        if(execution.definition.cooldownStart()==AbilityDefinition.CooldownStart.EXECUTION_END)startCooldowns(execution);
    }
    private static AbilityAvailability denied(AbilityAvailability.Reason reason){return new AbilityAvailability(false,reason,0,Optional.empty());}
    public AbilityAvailability evaluate(AbilityRequest request){return evaluate(request,DriverScope.ACTIVE,false);}
    private AbilityAvailability evaluate(AbilityRequest request,DriverScope scope,boolean continuation){
        world.requireActive();Objects.requireNonNull(request);
        var grant=request.grant();
        if(!current(grant))return denied(AbilityAvailability.Reason.STALE_GRANT);
        var definition=definition(grant);
        if(definition==null)return denied(AbilityAvailability.Reason.STALE_GRANT);
        if(!definition.targets().contains(request.target().kind()))return denied(AbilityAvailability.Reason.TARGET_CONTRACT);
        if(!world.driverControl().allowsAutomatic(grant.actor().entity(),scope,sourceKey(grant)))return denied(AbilityAvailability.Reason.EXTERNAL_OWNER);
        if((world.effects().restrictionMask(grant.actor().entity())&(1<<RestrictedAction.CAST.ordinal()))!=0)return denied(AbilityAvailability.Reason.RESTRICTED);
        if(!continuation&&slots(grant,definition).stream().anyMatch(key->occupying(key).isPresent()))return new AbilityAvailability(false,AbilityAvailability.Reason.COOLDOWN,remaining(grant,definition),Optional.empty());
        if(!continuation){long remaining=remaining(grant,definition);if(remaining>0)return new AbilityAvailability(false,AbilityAvailability.Reason.COOLDOWN,remaining,Optional.empty());}
        try{
            var actor=world.participantInput(grant.actor().entity(),grant.generation());
            if(actor.isEmpty()||!world.targetAlive(grant.actor().entity()))return denied(AbilityAvailability.Reason.TARGET_UNAVAILABLE);
            var targets=new ArrayList<io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame>();
            for(var id:targetIds(request)){
                if(!world.targetAlive(id))return denied(AbilityAvailability.Reason.TARGET_UNAVAILABLE);
                targets.add(id.equals(grant.actor().entity())?actor.orElseThrow():world.participantInput(id,grant.generation()).orElseThrow());
            }
            var input=new AbilityInput(request,definition,actor.orElseThrow(),targets);
            try(var computation=CombatReadScope.open()){
                if(!AbilityExecutors.require(definition.executor()).available().test(input))return new AbilityAvailability(false,AbilityAvailability.Reason.EXECUTOR_REFUSED,0,Optional.of(input));
            }
            return new AbilityAvailability(true,AbilityAvailability.Reason.AVAILABLE,0,Optional.of(input));
        }catch(RuntimeException failure){return denied(AbilityAvailability.Reason.CAPTURE_FAILED);}
    }
    private static List<UUID> targetIds(AbilityRequest request){return switch(request.target()){
        case AbilityTarget.Self ignored -> List.of(request.grant().actor().entity());
        case AbilityTarget.Entity target -> List.of(target.entity());
        case AbilityTarget.Entities targets -> targets.entities();
        case AbilityTarget.Position ignored -> List.of();
    };}
    public AbilityResult execute(AbilityRequest request){return execute(request,null);}
    public AbilityResult execute(AbilityRequest request,CombatParent parent){return execute(request,parent,DriverScope.ACTIVE);}
    public AbilityResult externalTrigger(AbilityRequest request){
        var definition=definition(request.grant());
        if(definition==null||definition.trigger().kind()==AbilityTrigger.Kind.ACTIVE)return new AbilityResult(Optional.empty(),CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Expected automatic ability definition"));
        return execute(request,null,definition.trigger().kind()==AbilityTrigger.Kind.PERIODIC?DriverScope.PERIODIC:DriverScope.TRIGGER,true);
    }
    public DriverScope executionScope(UUID id){world.requireActive();return Objects.requireNonNull(storage.executions.get(id),"Unknown ability execution").scope;}
    public AbilityResult resumeExternal(UUID actor,UUID id){world.requireActive();var value=storage.executions.get(id);if(value==null)return new AbilityResult(Optional.empty(),CommandResult.rejected(CommandRejection.Code.NOT_FOUND,"Unknown ability execution"));return resume(actor,id,value.step+1);}
    private AbilityResult execute(AbilityRequest request,CombatParent parent,DriverScope scope){
        return execute(request,parent,scope,false);
    }
    private AbilityResult execute(AbilityRequest request,CombatParent parent,DriverScope scope,boolean externalTrigger){
        var evaluation=evaluate(request,scope,false);
        var receipt=StateCommands.<Boolean>submit(world,request.requestId(),request.grant().actor().entity(),CombatOperation.ABILITY,
            Optional.of(new CombatSource(sourceKey(request.grant()),Optional.of(request.grant().actor().entity()),Optional.empty())),parent,()->{
                if(storage.executions.containsKey(request.requestId()))return CommandResult.rejected(CommandRejection.Code.DUPLICATE,"Ability execution already exists");
                if(!evaluation.available())return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Ability unavailable: "+evaluation.reason());
                if(scope==DriverScope.ACTIVE&&evaluation.input().orElseThrow().definition().trigger().kind()!=AbilityTrigger.Kind.ACTIVE)return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Automatic ability requires its trigger owner");
                if(!current(request.grant()))return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE,"Grant changed during capture");
                trim();if(storage.executions.size()>=256)return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Ability execution capacity exceeded");
                var definition=evaluation.input().orElseThrow().definition();
                var cooldownSlots=slots(request.grant(),definition);
                if(remaining(request.grant(),definition)>0||cooldownSlots.stream().anyMatch(key->occupying(key).isPresent()))return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Cooldown changed during capture");
                if(storage.cooldowns.size()+cooldownSlots.stream().filter(key->!storage.cooldowns.containsKey(key)).count()>4096)return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Ability cooldown capacity exceeded");
                var bound=targetIds(request).stream().map(id->world.participantIdentity(id).orElseThrow()).toList();
                var execution=new Execution(request,new CombatAction(UUID.randomUUID(),world.sessionId(),request.grant().actor().entity()),bound,scope);
                execution.definition=definition;execution.cooldownSlots=cooldownSlots;
                if(externalTrigger){
                    var source=world.behaviors().capture(request.grant().actor().entity()).stream()
                        .filter(s->s.source().equals(sourceKey(request.grant()))&&s.effect().equals(AbilityBehaviors.effectId(request.grant().contribution()))).findFirst();
                    if(source.isEmpty())return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE,"Trigger source absent");
                    var refusal=world.claimAbilityTrigger(source.orElseThrow(),request.grant().contribution(),definition.trigger().limit(),execution.action);
                    if(refusal.isPresent())return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Trigger limit: "+refusal.orElseThrow());
                    if(scope==DriverScope.PERIODIC)world.consumeAbilityPeriodic(source.orElseThrow(),request.grant().contribution());
                }
                world.decisionSamples().retain(execution.action.id(),samplesOwner(execution));
                storage.executions.put(request.requestId(),execution);
                if(definition.cooldownStart()==AbilityDefinition.CooldownStart.ADMISSION)startCooldowns(execution);
                fact(execution,AbilityFact.Kind.ADMITTED);
                return run(execution,evaluation.input().orElseThrow());
            });
        return new AbilityResult(execution(request.requestId()),receipt);
    }
    public AbilityResult activate(UUID actor,NexusId source,NexusId contribution,CombatParent parent,DriverScope scope){
        world.requireActive();
        var grant=grants(actor).stream().filter(g->sourceKey(g).equals(source)&&g.contribution().equals(contribution)).findFirst();
        if(grant.isEmpty())return new AbilityResult(Optional.empty(),CommandResult.rejected(CommandRejection.Code.STALE_HANDLE,"Ability source no longer active"));
        return execute(new AbilityRequest(UUID.randomUUID(),grant.orElseThrow(),new AbilityTarget.Self(),AbilityParameters.EMPTY),parent,scope);
    }
    private CommandResult<Boolean> run(Execution execution,AbilityInput input){
        int firstCommand=execution.commands.size();
        var declaration=AbilityExecutors.require(input.definition().executor());
        var allowed=EnumSet.noneOf(CombatOperation.class);allowed.addAll(declaration.spec().permissions());allowed.add(CombatOperation.ABILITY);
        CombatExecutionScope.currentLineage(world.serverSession()).orElseThrow().restrictOperations(allowed);
        if(!bindingsCurrent(execution))return finishRejected(execution,"Target instance changed");
        var callback=new Callback(execution,input);
        boolean complete=true;
        try(var action=CombatActionScope.resume(world,execution.action)){
            var next=Objects.requireNonNull(declaration.execute().apply(callback));
            if(!current(execution.request.grant())){execution.status=AbilitySnapshot.Status.CANCELLED;}
            else if(next instanceof AbilityStepResult.Delay delay){
                execution.waiting(delay.continuation(),world.businessTick(execution.request.grant().actor().entity(),sourceKey(execution.request.grant())));
                schedule(execution);
            }else if(next instanceof AbilityStepResult.AwaitInput window && (execution.invocation!=AbilityExecution.Invocation.INPUT||execution.remainingInputs>0)){
                execution.awaitInput(window,world.businessTick(execution.request.grant().actor().entity(),sourceKey(execution.request.grant())));
                schedule(execution);
            }else {execution.closeReason=Optional.of(AbilitySnapshot.CloseReason.COMPLETED);execution.terminate(AbilitySnapshot.Status.COMPLETED);}
        }catch(Stopped failure){complete=false;execution.failed();}
        catch(RuntimeException failure){complete=false;execution.failed();}
        finally{if(execution.status==AbilitySnapshot.Status.PARTIAL||execution.status==AbilitySnapshot.Status.UNKNOWN)execution.closeReason=Optional.of(AbilitySnapshot.CloseReason.FAILED);callback.open=false;if(execution.status!=AbilitySnapshot.Status.WAITING){finishCooldowns(execution);releaseSamples(execution);}}
        var journal=execution.journal(firstCommand,complete);
        if(!journal.applicationCompleted()){terminal(execution);return CommandResult.<Boolean>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION,"Ability execution incomplete; confirmed writes retained").withJournal(journal);}
        if(execution.commands.stream().anyMatch(e->!e.applied())){
            if(execution.status==AbilitySnapshot.Status.WAITING){
                ((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)world.level().getEntity(execution.request.grant().actor().entity())).combatnexus$schedules().remove(execution.request.requestId());
                releaseSamples(execution);
            }
            execution.refused();terminal(execution);
            return CommandResult.<Boolean>rejected(CommandRejection.Code.NATIVE_REFUSED,"Ability child command refused").withJournal(journal);
        }
        if(execution.status!=AbilitySnapshot.Status.WAITING)terminal(execution);
        return CommandResult.applied(true).withJournal(journal);
    }
    private boolean bindingsCurrent(Execution execution){return current(execution.request.grant())&&execution.targets.stream().allMatch(t->world.participantIdentity(t.entity()).filter(t::equals).isPresent()&&world.targetAlive(t.entity()));}
    private CommandResult<Boolean> finishRejected(Execution execution,String reason){finishCooldowns(execution);releaseSamples(execution);execution.terminate(AbilitySnapshot.Status.REJECTED);terminal(execution);return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE,reason);}
    private void schedule(Execution execution){
        var actor=execution.request.grant().actor().entity();
        var store=((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)world.level().getEntity(actor)).combatnexus$schedules();
        store.add(new io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledTask(execution.request.requestId(),
            new CombatSource(sourceKey(execution.request.grant()),Optional.of(actor),Optional.empty()),Optional.of(execution.request.grant().source().instance()),execution.next.orElseThrow(),1,1,
            new AbilityContinuationIntent(execution.request.requestId(),execution.step+1),
            EnumSet.of(io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.DEATH,
                io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.RESPAWN,
                io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.SOURCE_REMOVED,
                io.github.fatalitaya.combatnexus.combat.state.api.StateKey.Boundary.DIMENSION_CHANGE),false));
    }
    public AbilityResult resume(UUID actor,UUID id,int step){
        world.requireActive();var execution=storage.executions.get(id);
        if(execution==null||!execution.mayResume(actor,step))
            return new AbilityResult(execution(id),CommandResult.rejected(CommandRejection.Code.DUPLICATE,"Continuation already consumed or absent"));
        if(execution.waiting!=AbilitySnapshot.Waiting.INPUT&&!world.driverControl().allowsAutomatic(execution.request.grant().actor().entity(),execution.scope,sourceKey(execution.request.grant())))
            return new AbilityResult(Optional.of(execution.snapshot()),CommandResult.rejected(CommandRejection.Code.INVALID_PHASE,"Continuation is externally owned"));
        if(world.businessTick(actor,sourceKey(execution.request.grant()))<execution.next.orElseThrow())
            return new AbilityResult(Optional.of(execution.snapshot()),CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Continuation is not due"));
        var evaluation=execution.waiting==AbilitySnapshot.Waiting.INPUT?null:evaluate(execution.request,execution.scope,true);
        var receipt=StateCommands.<Boolean>submit(world,UUID.randomUUID(),execution.request.grant().actor().entity(),CombatOperation.ABILITY,Optional.of(new CombatSource(sourceKey(execution.request.grant()),Optional.of(actor),Optional.empty())),null,()->{
            if(execution.waiting==AbilitySnapshot.Waiting.INPUT){
                cancelPending(execution,AbilitySnapshot.CloseReason.EXPIRED);
                return CommandResult.applied(true);
            }
            if(execution.restored&&evaluation.available()){
                execution.targets=targetIds(execution.request).stream().map(target->world.participantIdentity(target).orElseThrow()).toList();execution.restored=false;
            }
            if(!evaluation.available()||!bindingsCurrent(execution)){cancelPending(execution);return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE,"Continuation grant or target unavailable");}
            ((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)world.level().getEntity(actor)).combatnexus$schedules().remove(id);
            execution.resume(step);execution.invocation=AbilityExecution.Invocation.AUTOMATIC;
            return run(execution,evaluation.input().orElseThrow());
        });
        return new AbilityResult(Optional.of(execution.snapshot()),receipt);
    }
    public AbilityResult reinput(AbilityReinput request){return reinput(request,null);}
    public AbilityResult reinput(AbilityReinput request,CombatParent parent){
        var receipt=StateCommands.<Boolean>submit(world,request.requestId(),request.actor(),CombatOperation.ABILITY,parent,()->{
            var execution=storage.executions.get(request.execution());
            if(execution==null||!execution.request.grant().actor().entity().equals(request.actor()))return CommandResult.rejected(CommandRejection.Code.NOT_FOUND,"Ability execution absent");
            if(execution.status!=AbilitySnapshot.Status.WAITING||execution.waiting!=AbilitySnapshot.Waiting.INPUT)return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Ability is not awaiting input");
            if(world.businessTick(request.actor(),sourceKey(execution.request.grant()))>=execution.next.orElseThrow()){
                cancelPending(execution,AbilitySnapshot.CloseReason.EXPIRED);
                return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Input window expired");
            }
            var input=new AbilityRequest(request.requestId(),execution.request.grant(),request.target(),request.input());
            var evaluation=evaluate(input,execution.scope,true);
            if(!evaluation.available())return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Input unavailable: "+evaluation.reason());
            if(!current(execution.request.grant())||execution.status!=AbilitySnapshot.Status.WAITING||execution.remainingInputs<=0||execution.step>=63)return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE,"Input window changed");
            ((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)world.level().getEntity(request.actor())).combatnexus$schedules().remove(request.execution());
            execution.remainingInputs--;execution.step++;execution.status=AbilitySnapshot.Status.RUNNING;execution.invocation=AbilityExecution.Invocation.INPUT;
            execution.request=new AbilityRequest(execution.request.requestId(),execution.request.grant(),request.target(),request.input());
            execution.targets=targetIds(input).stream().map(id->world.participantIdentity(id).orElseThrow()).toList();
            return run(execution,evaluation.input().orElseThrow());
        });
        return new AbilityResult(execution(request.execution()),receipt);
    }
    public boolean mayResume(UUID id){world.requireActive();var value=storage.executions.get(id);return value!=null&&(value.waiting==AbilitySnapshot.Waiting.INPUT||world.driverControl().allowsAutomatic(value.request.grant().actor().entity(),value.scope,sourceKey(value.request.grant())));}
    public CommandResult<Boolean> cancel(UUID request,UUID actor,UUID id){return StateCommands.submit(world,request,actor,CombatOperation.ABILITY,null,()->{
        var value=storage.executions.get(id);if(value==null||!value.request.grant().actor().entity().equals(actor))return CommandResult.rejected(CommandRejection.Code.NOT_FOUND,"Ability execution absent");
        if(value.status!=AbilitySnapshot.Status.WAITING)return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Ability execution is not waiting");
        cancelPending(value);return CommandResult.applied(true);
    });}
    private void cancelPending(Execution execution){cancelPending(execution,AbilitySnapshot.CloseReason.CANCELLED);}
    private void cancelPending(Execution execution,AbilitySnapshot.CloseReason reason){
        var entity=world.level().getEntity(execution.request.grant().actor().entity());
        if(entity instanceof io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules schedules)schedules.combatnexus$schedules().remove(execution.request.requestId());
        execution.closeReason=Optional.of(reason);execution.terminate(AbilitySnapshot.Status.CANCELLED);terminal(execution);releaseSamples(execution);
    }
    private static DecisionSamples.Owner samplesOwner(Execution execution){return new DecisionSamples.Owner(execution.request.requestId(),0);}
    private void releaseSamples(Execution execution){if(world.decisionSamples().release(execution.action.id(),samplesOwner(execution)))world.participantStates().actionFinished(execution.action.actor(),execution.action.id());}
    public Optional<AbilitySnapshot> execution(UUID id){world.requireActive();return Optional.ofNullable(storage.executions.get(id)).map(Execution::snapshot);}
    public List<SavedAbility> save(UUID actor,Set<NexusId> persistentSources){
        world.requireActive();return storage.executions.values().stream().filter(e->e.request.grant().actor().entity().equals(actor)&&e.status==AbilitySnapshot.Status.WAITING
            &&e.waiting==AbilitySnapshot.Waiting.DELAY&&current(e.request.grant())&&persistentSources.contains(sourceKey(e.request.grant())))
            .map(e->e.save(definition(e.request.grant()),world.businessTick(actor,sourceKey(e.request.grant())),world.decisionSamples().snapshot(e.action.id()))).toList();
    }
    public List<SavedAbilityCooldown> saveCooldowns(UUID actor) {
        world.requireActive();
        return storage.saveCooldowns(actor, clock -> world.businessTick(actor,clock));
    }
    public void restore(UUID actor,List<SavedAbility> saved,List<SavedAbilityCooldown> savedCooldowns){
        world.requireActive();CombatMutationGuard.requireWorldWritePhase();
        for(var entry:savedCooldowns)storage.restoreCooldown(actor,entry,world.businessTick(actor,entry.clock()));
        for(var entry:saved){
            var grant=storage.grants.get(new Key(actor,entry.source(),entry.contribution()));
            if(grant==null||!current(grant)||!entry.definition().equals(definition(grant)))throw new IllegalArgumentException("Saved ability binding unavailable or changed");
            if(storage.executions.containsKey(entry.execution()))throw new IllegalArgumentException("Duplicate saved ability execution");
            var execution=Execution.restore(grant,world.sessionId(),entry,world.businessTick(actor,sourceKey(grant)));
            execution.definition=entry.definition();execution.cooldownSlots=slots(grant,entry.definition());
            world.decisionSamples().restore(execution.action.id(),samplesOwner(execution),entry.samples());
            storage.executions.put(entry.execution(),execution);schedule(execution);
        }
    }
    private void trim() {
        storage.trim(key -> world.businessTick(key.actor(),key.clock()));
    }
    public void prepareLeave(UUID actor,boolean death){
        world.requireActive();CombatMutationGuard.requireWorldWritePhase();
        for(var execution:storage.executions.values())if(execution.request.grant().actor().entity().equals(actor)&&execution.status==AbilitySnapshot.Status.WAITING
                &&(death||execution.waiting==AbilitySnapshot.Waiting.INPUT)){
            cancelPending(execution,AbilitySnapshot.CloseReason.LIFECYCLE);
        }
        if(death)storage.cooldowns.keySet().removeIf(key->key.actor().equals(actor)&&storage.clearOnDeath.contains(key));
    }
    public void clear(UUID actor){for(var execution:storage.executions.values())if(execution.request.grant().actor().entity().equals(actor)&&execution.status==AbilitySnapshot.Status.WAITING)cancelPending(execution);storage.grants.keySet().removeIf(k->k.actor().equals(actor));storage.clearOnDeath.removeIf(k->k.actor().equals(actor));storage.cooldowns.keySet().removeIf(k->k.actor().equals(actor));}
    public void close(){for(var execution:storage.executions.values())releaseSamples(execution);storage.grants.clear();storage.cooldowns.clear();storage.clearOnDeath.clear();storage.executions.clear();}
    private static final class Stopped extends RuntimeException {}
    private final class Callback implements AbilityExecution {
        final Execution execution;final AbilityInput input;final Thread owner=Thread.currentThread();boolean open=true;
        Callback(Execution execution,AbilityInput input){this.execution=execution;this.input=input;}
        private void check(){world.requireActive();if(!open||Thread.currentThread()!=owner||!bindingsCurrent(execution))throw new IllegalStateException("Ability command capability expired");}
        public void released(){check();if(!execution.released){execution.released=true;fact(execution,AbilityFact.Kind.RELEASED);}}
        public Invocation invocation(){check();return execution.invocation;}
        public AbilityInput input(){check();return input;}
        private CombatAction attackAction;
        public CombatAction action(){check();return attackAction == null ? execution.action : attackAction;}
        public void attackAction(Runnable attack){
            check();Objects.requireNonNull(attack);
            var previous=attackAction;
            try(var scope=CombatActionScope.open(world,execution.action.actor())){
                attackAction=CombatActionScope.current(world.sessionId(),execution.action.actor()).orElseThrow();
                attack.run();
            }finally{attackAction=previous;}
        }
        public int step(){check();return execution.step;}
        public AbilityParameters state(){check();return execution.state;}
        public CombatSource source(){check();return new CombatSource(sourceKey(execution.request.grant()),Optional.of(execution.request.grant().actor().entity()),Optional.empty());}
        public long businessTick(){check();return world.businessTick(execution.request.grant().actor().entity(),sourceKey(execution.request.grant()));}
        public long businessTick(UUID target,NexusId source){check();return world.businessTick(target,source);}
        private CommandOutcome<?> record(CommandOutcome<?> result){if(!execution.record(result)){open=false;throw new Stopped();}return result;}
        private void capacity(){check();if(execution.commands.size()>=256)throw new IllegalStateException("Ability command budget exceeded");}
        public CommandOutcome<?> submit(DomainCommand command){capacity();return record(DomainCommandExecutor.execute(world,command));}
        public CommandOutcome<?> damage(UUID target,double amount,NexusId type){capacity();return record(DomainCommandExecutor.damage(world,target,source(),amount,type));}
    }
}
