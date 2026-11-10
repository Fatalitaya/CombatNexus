package io.github.fatalitaya.combatnexus.combat.internal.definition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.definition.*;
import io.github.fatalitaya.combatnexus.config.DefinitionResources;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
/** Inert typed defaults. The aggregate reader owns all decoding and semantic compilation. */
public final class JavaCombatDefinitions implements CombatDefinitionRegistrar {
    private boolean frozen;
    private final Map<NexusId,io.github.fatalitaya.combatnexus.combat.ability.api.AbilityDefinition> abilities=new LinkedHashMap<>();
    private final Map<NexusId,BehaviorSpec> behaviors=new LinkedHashMap<>();
    private final Map<NexusId,EffectSpec> effects=new LinkedHashMap<>();
    private final Map<NexusId,EntryTemplateSpec> entries=new LinkedHashMap<>();
    private final Map<NexusId,AffixTemplateSpec> affixes=new LinkedHashMap<>();
    private <T> void add(Map<NexusId,T> map,NexusId id,T spec){
        if(frozen)throw new IllegalStateException("Definitions frozen");
        Objects.requireNonNull(id);Objects.requireNonNull(spec);
        if(id.toString().length()>256 || map.size()>=256)throw new IllegalArgumentException("Definition budget");
        if(map.putIfAbsent(id,spec)!=null)throw new IllegalArgumentException("Duplicate definition: "+id);
    }
    public void behavior(NexusId id,BehaviorSpec spec){add(behaviors,id,spec);}
    public void ability(NexusId id,io.github.fatalitaya.combatnexus.combat.ability.api.AbilityDefinition definition){if(!id.equals(definition.id()))throw new IllegalArgumentException("Ability identity mismatch");add(abilities,id,definition);}
    public void effect(NexusId id,EffectSpec spec){add(effects,id,spec);}
    public void entryTemplate(NexusId id,EntryTemplateSpec spec){if(!id.equals(spec.definition().id()))throw new IllegalArgumentException("Template identity mismatch");add(entries,id,spec);}
    public void affixTemplate(NexusId id,AffixTemplateSpec spec){if(!id.equals(spec.definition().id()))throw new IllegalArgumentException("Template identity mismatch");add(affixes,id,spec);}
    public void freeze(){frozen=true;}
    private static JsonArray ids(Collection<?> values){var array=new JsonArray();values.forEach(v->array.add(v.toString()));return array;}
    private static JsonObject encode(BehaviorSpec value){
        var json=new JsonObject();json.addProperty("priority",value.priority());var lifetime=new JsonObject();lifetime.addProperty("kind",value.lifetime().kind().name());if(value.lifetime().kind()==io.github.fatalitaya.combatnexus.effect.api.EffectLifetime.Kind.TIMED)lifetime.addProperty("ticks",value.lifetime().ticks());json.add("lifetime",lifetime);
        var components=new JsonArray();value.components().forEach(c->components.add(io.github.fatalitaya.combatnexus.combat.internal.registration.BehaviorTypes.encode(c)));json.add("components",components);return json;
    }
    private static JsonObject encode(EffectSpec value){
        var json=new JsonObject();json.addProperty("level",value.level());json.addProperty("priority",value.priority());json.addProperty("stacking",value.stacking().toString());
        var lifetime=new JsonObject();lifetime.addProperty("kind",value.lifetime().kind().name());if(value.lifetime().kind()==io.github.fatalitaya.combatnexus.effect.api.EffectLifetime.Kind.TIMED)lifetime.addProperty("ticks",value.lifetime().ticks());json.add("lifetime",lifetime);
        json.add("control",value.policy().control().encode());json.addProperty("category",value.policy().category().name());json.addProperty("dispellable",value.policy().dispellable());json.add("tags",ids(value.policy().tags()));json.add("behaviors",ids(value.behaviors()));json.add("restrictions",ids(value.restrictions()));
        var attributes=new JsonArray();for(var a:value.attributes()){var j=new JsonObject();j.addProperty("attribute",a.attribute().toString());j.addProperty("modifier",a.modifier().source().toString());j.addProperty("amount",a.modifier().amount());j.addProperty("operation",a.modifier().operation().name());attributes.add(j);}json.add("attributes",attributes);
        var d=value.display();if(!d.equals(io.github.fatalitaya.combatnexus.effect.api.EffectDisplay.NONE)){
            if(d.nativeId()==null || d.owner()!=null || !d.provenance().equals(io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance.entity(null)))throw new IllegalArgumentException("Definition display must have a native ID and no installation provenance");
            var j=new JsonObject();j.addProperty("native_id",d.nativeId().toString());j.addProperty("ambient",d.ambient());j.addProperty("particles",d.particles());j.addProperty("icon",d.icon());j.addProperty("aggregate_sources",d.aggregateSources());j.addProperty("persistent",d.persistent());j.addProperty("environment",d.environment());json.add("display",j);
        }return json;
    }
    public DefinitionResources overlay(DefinitionResources packs){
        if(!frozen)throw new IllegalStateException("Definitions not frozen");
        return new DefinitionResources(){
            public Map<NexusId,Resource> json(String directory){
                var result=new LinkedHashMap<NexusId,Resource>();
                switch(directory){
                    case "combatnexus_abilities" -> abilities.forEach((id,v)->put(result,directory,id,v::encode));
                    case "combatnexus_behaviors" -> behaviors.forEach((id,v)->put(result,directory,id,()->encode(v)));
                    case "combatnexus_effects" -> effects.forEach((id,v)->put(result,directory,id,()->encode(v)));
                    case "combatnexus_entry_templates" -> entries.forEach((id,v)->put(result,directory,id,()->io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition.CODEC.encodeStart(JsonOps.INSTANCE,v.definition()).getOrThrow()));
                    case "combatnexus_affix_templates" -> affixes.forEach((id,v)->put(result,directory,id,()->io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition.CODEC.encodeStart(JsonOps.INSTANCE,v.definition()).getOrThrow()));
                    default -> {}
                }
                result.putAll(packs.json(directory));return Map.copyOf(result);
            }
            public Resource required(NexusId file)throws java.io.IOException{return packs.required(file);}
            public boolean contains(NexusId file){return packs.contains(file);}
        };
    }
    private static void put(Map<NexusId,DefinitionResources.Resource> result,String directory,NexusId id,java.util.function.Supplier<JsonElement> encode){
        result.put(new NexusId(id.namespace(),directory+"/"+id.path()+".json"),()->new java.io.StringReader(encode.get().toString()));
    }
}
