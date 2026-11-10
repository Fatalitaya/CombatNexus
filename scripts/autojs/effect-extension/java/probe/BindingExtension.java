package probe;
import io.github.fatalitaya.combatnexus.combat.api.CombatRegistrar;
import io.github.fatalitaya.combatnexus.combat.api.definition.*;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.effect.api.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class BindingExtension {
    private static CombatDefinitionRegistrar retained;
    private static CombatRegistrar registrar;
    public static boolean duplicateRejected, repeatedDefaultsRejected;
    public static NexusId id(String path){return NexusId.parse("combatnexus_effect_probe:"+path);}
    public static void items(IEventBus bus){
        var items=DeferredRegister.createItems("combatnexus_effect_probe");
        items.registerItem("bound_item",properties->{
            CombatItems.defaults(properties,b->b.entryTemplate(id("default"),id("effect_entry"),EquipmentPosition.MAINHAND,EquipmentPosition.OFFHAND));
            CombatItems.defaults(properties,b->b.entryTemplate(id("guard"),id("guard_entry"),EquipmentPosition.MAINHAND));
            try{CombatItems.defaults(properties,b->b.entryTemplate(id("default"),id("effect_entry"),EquipmentPosition.MAINHAND));}catch(IllegalArgumentException expected){repeatedDefaultsRejected=true;}
            return new Item(properties);
        });items.register(bus);
    }
    public static void definitions(CombatRegistrar api){
        registrar=api;retained=api.definitions();
        retained.behavior(id("java_behavior"),b->b.priority(7).duration(400).component(new EffectExtension.Pulse(2,20)));
        retained.effect(id("java_effect"),b->b.lifetime(EffectLifetime.permanent()).behavior(id("java_behavior")));
        try{retained.behavior(id("java_behavior"),b->b.duration(10));}catch(IllegalArgumentException expected){duplicateRejected=true;}
        var rule=io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString("{\"id\":\"combatnexus_effect_probe:rule\",\"phase\":\"base_modification\",\"operations\":[{\"type\":\"combatnexus:add_base_damage\",\"channel\":\"combatnexus:physical\",\"value\":3}]} ")).getOrThrow();
        retained.entryTemplate(id("effect_entry"),b->b.effect(id("effect"),id("java_effect")));
        retained.entryTemplate(id("guard_entry"),b->b.behavior(id("guard"),NexusId.parse("combatnexus:last_stand")));
        var entry=new EntryTemplateSpec.Builder(id("java_template")).rule(id("damage"),rule,io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter.ITEM).build();
        retained.entryTemplate(id("java_template"),entry);
        retained.affixTemplate(id("java_template"),b->b.entry(id("member"),entry.definition()));
        api.itemBindings(id("stick_contribution"),NexusId.parse("minecraft:stick"),new CombatItemBindingBuilder().entryTemplate(id("java_binding"),id("effect_entry"),EquipmentPosition.MAINHAND).build());
    }
    public static boolean frozen(){try{retained.behavior(id("late"),new BehaviorSpec.Builder().build());return false;}catch(IllegalStateException expected){return true;}}
    public static boolean bindingsFrozen(){try{registrar.itemBindings(id("late"),NexusId.parse("minecraft:stick"),CombatItemBindingSpec.EMPTY);return false;}catch(IllegalStateException expected){return true;}}
}
