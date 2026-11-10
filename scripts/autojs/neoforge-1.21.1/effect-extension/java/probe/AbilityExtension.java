package probe;

import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.ability.api.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItems;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import io.github.fatalitaya.combatnexus.effect.api.*;
import java.util.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Public API content extension used only by the existing AutoJS development source set. */
public final class AbilityExtension {
    private AbilityExtension() {}
    private static final List<UUID> ACTIONS = new ArrayList<>();
    private static final List<UUID> TARGETS = new ArrayList<>();
    private static AbilityExecution retained;
    private static CombatRegistrar registrar;
    public static int executions;
    public static CombatSource healingSourceOverride;
    public static io.github.fatalitaya.combatnexus.combat.driver.api.DriverSession periodicSession;
    public static NexusId id(String path) { return new NexusId("combatnexus_effect_probe",path); }
    public static List<UUID> actions() { return List.copyOf(ACTIONS); }
    public static List<UUID> targets() { return List.copyOf(TARGETS); }
    public static void reset() { ACTIONS.clear(); TARGETS.clear(); executions=0; }
    public static boolean expiredContext() {
        try { retained.businessTick(); return false; }
        catch (IllegalStateException expected) { return true; }
    }
    public static boolean frozenRegistration() {
        try { registrar.abilityExecutor(id("late_executor"),healer()); return false; }
        catch (IllegalStateException expected) { return true; }
    }
    public static void items(IEventBus bus) {
        var items=DeferredRegister.createItems("combatnexus_effect_probe");
        for(var kind:List.of("heal","shared","combo","delayed","periodic","damage_trigger")) {
            items.registerItem("ability_"+kind+"_wand",properties->{
                CombatItems.defaults(properties,b->b.entryTemplate(id("ability_binding"),id("ability_"+kind+"_entry"),EquipmentPosition.MAINHAND,EquipmentPosition.OFFHAND));
                return new Item(properties);
            });
        }
        items.register(bus);
    }
    private static void observe(AbilityExecution execution) {
        retained=execution; executions++; ACTIONS.add(execution.action().id());
        if(ACTIONS.size()>256) ACTIONS.removeFirst();
    }
    private static void heal(AbilityExecution execution,double amount) {
        for(var target:execution.input().targets()) {
            TARGETS.add(target.entity()); if(TARGETS.size()>256)TARGETS.removeFirst();
            execution.submit(new DomainCommand.Heal(new HealingRequest(UUID.randomUUID(),target.entity(),healingSourceOverride == null ? execution.source() : healingSourceOverride,HealingRequest.Reason.SKILL,amount)));
        }
        execution.released();
    }
    private static AbilityExecutor healer() {
        return new AbilityExecutor(new AbilityExecutorSpec(Set.of(AbilityTarget.Kind.SELF,AbilityTarget.Kind.ENTITY,AbilityTarget.Kind.ENTITIES),Set.of(CombatOperation.HEALING),1),
            input->true,execution->{observe(execution);heal(execution,execution.input().definition().parameters().number("amount"));return new AbilityStepResult.Complete();});
    }
    private static AbilityDefinition definition(String path,String executor,Set<AbilityTarget.Kind> targets,int cooldown,Optional<NexusId> shared,AbilityTrigger trigger) {
        return new AbilityDefinition(id(path),path,"Ability verification content",targets,id(executor),
            new AbilityParameters(Map.of("amount",3.0),Map.of()),cooldown,shared,shared.isPresent()?cooldown:0,AbilityDefinition.CooldownOwner.SOURCE_CONTRIBUTION,AbilityDefinition.CooldownStart.ADMISSION,AbilityDefinition.DeathCooldown.RETAIN,trigger);
    }
    public static void register(CombatRegistrar api) {
        registrar=api;GenericCombatExtension.register(api);
        api.abilityExecutor(id("heal_executor"),healer());
        api.abilityExecutor(id("combo_executor"),new AbilityExecutor(new AbilityExecutorSpec(Set.of(AbilityTarget.Kind.ENTITY,AbilityTarget.Kind.ENTITIES),Set.of(CombatOperation.DAMAGE,CombatOperation.EFFECT),1),
            input->true,execution->{
                observe(execution);
                for(var target:execution.input().targets()) {
                    TARGETS.add(target.entity());
                    execution.damage(target.entity(),execution.input().definition().parameters().number("amount"),NexusId.parse("minecraft:generic"));
                    var effect=ManagedEffects.agility(target.entity(),execution.source().id(),0.1,Math.addExact(execution.businessTick(target.entity(),execution.source().id()),40));
                    execution.submit(new DomainCommand.ManagedEffect(new EffectApplication(UUID.randomUUID(),effect,EffectService.Stacking.REPLACE)));
                }
                return new AbilityStepResult.Complete();
            }));
        api.abilityExecutor(id("delayed_executor"),new AbilityExecutor(healer().spec(),input->true,execution->{
            observe(execution);
            if(execution.step()==0)return new AbilityStepResult.Delay(new AbilityContinuation(5,new AbilityParameters(Map.of("amount",4.0),Map.of())));
            heal(execution,execution.state().number("amount"));return new AbilityStepResult.Complete();
        }));
        api.abilityExecutor(id("partial_executor"),new AbilityExecutor(healer().spec(),input->true,execution->{
            observe(execution);heal(execution,1);throw new IllegalStateException("Intentional failure after confirmed healing");
        }));
        api.abilityExecutor(id("unavailable_executor"),new AbilityExecutor(healer().spec(),input->false,execution->{
            throw new AssertionError("Unavailable executor was invoked");
        }));
        api.abilityExecutor(id("unknown_executor"),new AbilityExecutor(healer().spec(),input->true,execution->{
            observe(execution);throw new IllegalStateException("Intentional failure without confirmed domain evidence");
        }));
        var targets=Set.of(AbilityTarget.Kind.SELF,AbilityTarget.Kind.ENTITY,AbilityTarget.Kind.ENTITIES);
        var definitions=api.definitions();
        definitions.ability(id("ability_heal"),definition("ability_heal","heal_executor",targets,20,Optional.empty(),AbilityTrigger.ACTIVE));
        definitions.ability(id("ability_shared"),definition("ability_shared","heal_executor",targets,20,Optional.of(id("shared_healing")),AbilityTrigger.ACTIVE));
        definitions.ability(id("ability_combo"),definition("ability_combo","combo_executor",Set.of(AbilityTarget.Kind.ENTITY,AbilityTarget.Kind.ENTITIES),0,Optional.empty(),AbilityTrigger.ACTIVE));
        definitions.ability(id("ability_delayed"),definition("ability_delayed","delayed_executor",targets,10,Optional.empty(),AbilityTrigger.ACTIVE));
        definitions.ability(id("ability_partial"),definition("ability_partial","partial_executor",targets,20,Optional.empty(),AbilityTrigger.ACTIVE));
        definitions.ability(id("ability_unavailable"),definition("ability_unavailable","unavailable_executor",targets,20,Optional.empty(),AbilityTrigger.ACTIVE));
        definitions.ability(id("ability_unknown"),definition("ability_unknown","unknown_executor",targets,20,Optional.empty(),AbilityTrigger.ACTIVE));
        definitions.ability(id("ability_periodic"),definition("ability_periodic","heal_executor",Set.of(AbilityTarget.Kind.SELF),0,Optional.empty(),new AbilityTrigger(AbilityTrigger.Kind.PERIODIC,new TriggerLimit(TriggerLimit.Scope.SOURCE,1),10,10)));
        definitions.ability(id("ability_periodic_unlimited"),definition("ability_periodic_unlimited","heal_executor",Set.of(AbilityTarget.Kind.SELF),0,Optional.empty(),new AbilityTrigger(AbilityTrigger.Kind.PERIODIC,TriggerLimit.ONCE,10,10)));
        definitions.ability(id("ability_damage_trigger"),definition("ability_damage_trigger","heal_executor",Set.of(AbilityTarget.Kind.SELF),0,Optional.empty(),new AbilityTrigger(AbilityTrigger.Kind.DAMAGE,TriggerLimit.ONCE,0,0)));
        for(var kind:List.of("heal","shared","combo","delayed","periodic","damage_trigger"))
            definitions.entryTemplate(id("ability_"+kind+"_entry"),builder->builder.ability(id("grant"),id("ability_"+kind)));
    }
}
