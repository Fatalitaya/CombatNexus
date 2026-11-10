package io.github.fatalitaya.combatnexus.combat.internal.definition;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.config.*;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.serialization.JsonOps;
import java.util.*;
public final class ItemBindingDefinitions {
    public record Contribution(NexusId item,CombatItemBindingSpec bindings){}
    public record Resource(NexusId id,NexusId item,int priority,CombatItemBindingPatch operations){}
    private final Map<NexusId,Contribution> contributions=new TreeMap<>();
    private boolean frozen;
    public void add(NexusId id,NexusId item,CombatItemBindingSpec bindings){
        if(frozen)throw new IllegalStateException("Item bindings frozen");
        Objects.requireNonNull(id);Objects.requireNonNull(item);Objects.requireNonNull(bindings);
        if(id.toString().length()>256 || item.toString().length()>256 || contributions.size()>=256)throw new IllegalArgumentException("Item contribution budget");
        if(contributions.putIfAbsent(id,new Contribution(item,bindings))!=null)throw new IllegalArgumentException("Duplicate item contribution: "+id);
    }
    public void freeze(){frozen=true;}
    public static List<Resource> read(DefinitionResources manager){
        var files=manager.json("combatnexus_item_bindings");if(files.size()>256)throw new IllegalArgumentException("Item binding resource budget");
        var result=new ArrayList<Resource>();
        for(var entry:files.entrySet())try(var reader=entry.getValue().openAsReader()){
            var json=SettingsJson.fields(SettingsJson.read(reader),"item","priority","operations");
            int priority=SettingsJson.integer(json,"priority");DefinitionJson.range(priority,-10000,10000,"priority");
            var operations=new ArrayList<CombatItemBindingPatch.Operation>();
            for(var operation:DefinitionJson.array(json,"operations",144)){
                var object=operation.getAsJsonObject();
                SettingsJson.fields(object,object.get("action").getAsString().equals("REMOVE")?new String[]{"action","id"}:new String[]{"action","id","binding"});
                operations.add(CombatItemBindingPatch.OPERATION.parse(JsonOps.INSTANCE,object).getOrThrow());
            }
            result.add(new Resource(DefinitionResources.definitionId("combatnexus_item_bindings",entry.getKey()),DefinitionJson.id(json,"item"),priority,new CombatItemBindingPatch(false,operations)));
        }catch(java.io.IOException|RuntimeException failure){throw new IllegalArgumentException("Invalid item bindings: "+entry.getKey(),failure);}
        return result.stream().sorted(Comparator.comparingInt(Resource::priority).thenComparing(Resource::id)).toList();
    }
    public Map<NexusId,CombatItemBindingSpec> compile(List<Resource> resources,Map<NexusId,CombatItemBindingSpec> defaults){
        if(!frozen)throw new IllegalStateException("Item bindings not frozen");
        var result=new LinkedHashMap<NexusId,CombatItemBindingSpec>();
        defaults.forEach((id,spec)->{if(!spec.bindings().isEmpty())result.put(id,spec);});
        for(var value:contributions.values()){requireItem(value.item());result.put(value.item(),new CombatItemBindingPatch(false,value.bindings().bindings().stream().map(CombatItemBindingPatch.Operation::add).toList()).apply(result.getOrDefault(value.item(),CombatItemBindingSpec.EMPTY)));}
        for(var value:resources){requireItem(value.item());result.put(value.item(),value.operations().apply(result.getOrDefault(value.item(),CombatItemBindingSpec.EMPTY)));}
        return Map.copyOf(result);
    }
    private static void requireItem(NexusId id){if(!BuiltInRegistries.ITEM.containsKey(NativeIds.nativeId(id)))throw new IllegalArgumentException("Unknown binding item: "+id);}
}
