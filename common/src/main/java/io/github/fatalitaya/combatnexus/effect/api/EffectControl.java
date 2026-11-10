package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.config.*;
import com.google.gson.*;
import java.util.*;

/** Classification and source-owned immunity selectors are independent. */
public record EffectControl(Set<ControlCategory> categories,Set<NexusId> immuneEffects,Set<NexusId> immuneTags,Set<ControlCategory> immuneCategories) {
    private static final com.mojang.serialization.Codec<Set<ControlCategory>> CATEGORIES=io.github.fatalitaya.combatnexus.api.codec.CodecBudgets.boundedList(com.mojang.serialization.Codec.STRING.xmap(ControlCategory::valueOf,Enum::name),3,"Control categories").xmap(values->{if(new HashSet<>(values).size()!=values.size())throw new IllegalArgumentException("Duplicate category");return Set.copyOf(values);},values->values.stream().sorted().toList());
    private static final com.mojang.serialization.Codec<Set<NexusId>> IDS=io.github.fatalitaya.combatnexus.api.codec.CodecBudgets.boundedList(io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC,32,"Immunity ids").xmap(values->{if(new HashSet<>(values).size()!=values.size())throw new IllegalArgumentException("Duplicate immunity id");return Set.copyOf(values);},values->values.stream().sorted(Comparator.comparing(Object::toString)).toList());
    public static final com.mojang.serialization.Codec<EffectControl> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.create(i->i.group(
        CATEGORIES.fieldOf("categories").forGetter(EffectControl::categories),IDS.fieldOf("immune_effects").forGetter(EffectControl::immuneEffects),
        IDS.fieldOf("immune_tags").forGetter(EffectControl::immuneTags),CATEGORIES.fieldOf("immune_categories").forGetter(EffectControl::immuneCategories)).apply(i,EffectControl::new));
    public static final EffectControl NONE=new EffectControl(Set.of(),Set.of(),Set.of(),Set.of());
    public EffectControl {
        categories=Set.copyOf(categories);immuneEffects=Set.copyOf(immuneEffects);immuneTags=Set.copyOf(immuneTags);immuneCategories=Set.copyOf(immuneCategories);
        if(immuneEffects.size()>32||immuneTags.size()>32)throw new IllegalArgumentException("Immunity selector capacity");
    }
    public boolean matches(NexusId effect,Set<NexusId> tags,Set<ControlCategory> controls){
        return immuneEffects.contains(effect)||!Collections.disjoint(immuneTags,tags)||!Collections.disjoint(immuneCategories,controls);
    }
    private static Set<ControlCategory> categories(JsonObject json,String name){
        var result=EnumSet.noneOf(ControlCategory.class);
        if(json.has(name))for(var value:DefinitionJson.array(json,name,3))if(!result.add(ControlCategory.valueOf(value.getAsString())))throw new IllegalArgumentException("Duplicate control category");
        return Set.copyOf(result);
    }
    public static EffectControl decode(JsonObject json){
        json=json.deepCopy();for(var key:List.of("categories","immune_effects","immune_tags","immune_categories"))if(!json.has(key))json.add(key,new JsonArray());
        SettingsJson.fields(json,"categories","immune_effects","immune_tags","immune_categories");
        return new EffectControl(categories(json,"categories"),json.has("immune_effects")?DefinitionJson.ids(json,"immune_effects"):Set.of(),json.has("immune_tags")?DefinitionJson.ids(json,"immune_tags"):Set.of(),categories(json,"immune_categories"));
    }
    public JsonObject encode(){
        var result=new JsonObject();
        result.add("categories",array(categories));result.add("immune_effects",array(immuneEffects));result.add("immune_tags",array(immuneTags));result.add("immune_categories",array(immuneCategories));return result;
    }
    private static JsonArray array(Set<?> values){var result=new JsonArray();values.stream().map(Object::toString).sorted().forEach(result::add);return result;}
}
