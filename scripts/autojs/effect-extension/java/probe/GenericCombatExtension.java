package probe;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.ability.api.*;
import java.util.*;

public final class GenericCombatExtension {
    public static final List<AbilityFact> facts=new ArrayList<>();
    public static CombatWorld world;
    public static CombatParent retained;
    public static int consumed;
    public static int callbacks;
    public static boolean react;
    public static boolean failInput;
    public static NexusId id(String path){return AbilityExtension.id(path);}
    public static void register(CombatRegistrar api){
        api.observeAbility(id("generic_observer_failure"),10,fact->{throw new IllegalStateException("Observer probe");});
        api.observeAbility(id("generic_observer"),0,fact->{if(facts.size()==256)facts.remove(0);facts.add(fact);});
        api.reactAbility(id("generic_reaction"),0,Set.of(CombatOperation.STATE),callback->{
            if(!react||callback.result().kind()!=AbilityFact.Kind.RELEASED)return;
            retained=callback.childAuthority().orElseThrow();
            var address=new io.github.fatalitaya.combatnexus.combat.state.api.StateAddress<>(callback.result().caster().entity(),PlanExample.RETAINED_STATE,Optional.empty());
            var source=new CombatSource(id("generic_reaction"),Optional.empty(),Optional.empty());
            var first=world.states().take(UUID.randomUUID(),address,source,retained);
            if(first.value().orElseThrow().isPresent())consumed++;
            if(world.states().take(UUID.randomUUID(),address,source,retained).value().orElseThrow().isPresent())throw new IllegalStateException("State consumed twice");
        });
        var targets=Set.of(AbilityTarget.Kind.SELF,AbilityTarget.Kind.ENTITY,AbilityTarget.Kind.ENTITIES);
        api.abilityExecutor(id("window_executor"),new AbilityExecutor(new AbilityExecutorSpec(targets,Set.of(CombatOperation.STATE),1),input->input.request().input().numbers().getOrDefault("reject",0.0)==0,execution->{
            callbacks++;execution.released();
            if(execution.invocation()==AbilityExecution.Invocation.INPUT&&failInput)throw new IllegalStateException("Input failure");
            return new AbilityStepResult.AwaitInput(5,2,AbilityParameters.EMPTY);
        }));
        for(var name:List.of("window","window_peer","window_clear")){
            var definition=new AbilityDefinition(id(name),name,"",targets,id("window_executor"),AbilityParameters.EMPTY,10,Optional.of(id("window_group")),20,
                AbilityDefinition.CooldownOwner.PLAYER_SKILL,AbilityDefinition.CooldownStart.EXECUTION_END,
                name.equals("window_clear")?AbilityDefinition.DeathCooldown.CLEAR:AbilityDefinition.DeathCooldown.RETAIN,AbilityTrigger.ACTIVE);
            api.definitions().ability(id(name),definition);
        }
    }
}
