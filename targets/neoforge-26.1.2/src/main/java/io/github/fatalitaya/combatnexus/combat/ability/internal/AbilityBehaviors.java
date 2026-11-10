package io.github.fatalitaya.combatnexus.combat.ability.internal;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.*;
import io.github.fatalitaya.combatnexus.combat.ability.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations;
import io.github.fatalitaya.combatnexus.combat.internal.registration.BehaviorTypes;
import io.github.fatalitaya.combatnexus.effect.api.*;
import java.util.*;

/** Ability activation uses the existing behavior reaction ledger and periodic scheduler. */
public final class AbilityBehaviors {
    private AbilityBehaviors() {}
    public record Periodic(NexusId contribution,AbilityTrigger trigger) implements PeriodicBehavior {
        public static final MapCodec<Periodic> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            NexusIdCodecs.CODEC.fieldOf("contribution").forGetter(Periodic::contribution),AbilityTrigger.CODEC.fieldOf("trigger").forGetter(Periodic::trigger)).apply(i,Periodic::new));
        public Periodic { Objects.requireNonNull(contribution);Objects.requireNonNull(trigger);if(trigger.kind()!=AbilityTrigger.Kind.PERIODIC)throw new IllegalArgumentException("Expected periodic ability"); }
        public int intervalTicks(){return trigger.intervalTicks();}
        public int initialDelayTicks(){return trigger.firstDelayTicks();}
        public void execute(PeriodicExecution execution){execution.ability(contribution,trigger.limit());}
    }
    public record Triggered(NexusId contribution,NexusId executor,AbilityTrigger trigger) implements SettlementBehavior {
        public static final MapCodec<Triggered> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            NexusIdCodecs.CODEC.fieldOf("contribution").forGetter(Triggered::contribution),NexusIdCodecs.CODEC.fieldOf("executor").forGetter(Triggered::executor),
            AbilityTrigger.CODEC.fieldOf("trigger").forGetter(Triggered::trigger)).apply(i,Triggered::new));
        public Triggered { Objects.requireNonNull(contribution);Objects.requireNonNull(executor);Objects.requireNonNull(trigger);if(trigger.kind()!=AbilityTrigger.Kind.DAMAGE&&trigger.kind()!=AbilityTrigger.Kind.HEALING)throw new IllegalArgumentException("Expected settlement ability"); }
        public Set<CombatOperation> subscriptions(){return Set.of(trigger.kind()==AbilityTrigger.Kind.DAMAGE?CombatOperation.DAMAGE:CombatOperation.HEALING);}
        public Set<CombatOperation> permissions(){var result=EnumSet.noneOf(CombatOperation.class);result.addAll(AbilityExecutorCatalog.require(executor).permissions());result.add(CombatOperation.ABILITY);return Set.copyOf(result);}
        public ReactionResult settle(SettlementContext context){
            var result=context.ability().apply(contribution,trigger.limit());
            return new ReactionResult(result.receipt().applied()?"executed":"rejected",result.execution().map(e->"ability/"+e.execution()));
        }
    }
    public static void register(){
        BehaviorTypes.register(new BehaviorType<>(new NexusId("combatnexus","ability_periodic"),Periodic.class,Periodic.CODEC));
        BehaviorTypes.register(new BehaviorType<>(new NexusId("combatnexus","ability_trigger"),Triggered.class,Triggered.CODEC));
    }
    static NexusId effectId(NexusId contribution){return new NexusId("combatnexus","ability_activation/"+contribution.namespace()+"/"+contribution.path());}
    public static void install(CombatRuntime world,UUID actor,SourceInstance source,SourcePlan plan){
        for(var entry:plan.abilities().entrySet()){
            var definition=world.sourceInstallations().definitions(actor, source).domains().abilities().get(entry.getValue());
            if(definition==null)continue;
            var id=effectId(entry.getKey());var sourceId=SourceInstallations.key(source);
            if(definition.trigger().kind()==AbilityTrigger.Kind.ACTIVE){world.effects().remove(actor,id,sourceId,EffectService.Removal.REMOVED);continue;}
            BehaviorComponent component=definition.trigger().kind()==AbilityTrigger.Kind.PERIODIC
                ?new Periodic(entry.getKey(),definition.trigger()):new Triggered(entry.getKey(),definition.executor(),definition.trigger());
            var effect=new EffectDefinition(0,EffectLifetime.permanent(),EffectService.Stacking.REPLACE,new EffectPolicy(EffectPolicy.Category.NEUTRAL,false),
                List.of(),new BehaviorPlan(0,List.of(component)),Set.of()).instantiate(actor,id,sourceId,world.businessTick(actor,sourceId));
            if(!world.effects().bind(new EffectBinding(effect,true,EffectService.Stacking.REPLACE)))throw new IllegalStateException("Ability activation installation refused");
        }
    }
    public static void remove(CombatRuntime world,UUID actor,SourceInstance source,SourcePlan plan){
        for(var contribution:plan.abilities().keySet())world.effects().remove(actor,effectId(contribution),SourceInstallations.key(source),EffectService.Removal.REMOVED);
    }
}
