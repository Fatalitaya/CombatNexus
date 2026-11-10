package io.github.fatalitaya.combatnexus.combat.damage.core.definition;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.fatalitaya.combatnexus.combat.damage.registry.rule.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
/** Reject misspelled fields before codecs apply their defaults; extension codecs declare their own keys. */
public final class DamageDefinitionFields {
    private DamageDefinitionFields() {}
    public static void validate(String directory, JsonObject json) {
        switch (directory) {
            case "combatnexus_channels" -> keys(json, "channel", "trigger_tags", "resistance_attribute", "affected_by_armor", "priority");
            case "combatnexus_rules" -> rule(json);
            case "combatnexus_entry_templates" -> entry(json);
            case "combatnexus_affix_templates" -> {
                keys(json,"id","display","rarity","members","stacking","stacking_group");
                if(json.has("display"))display(json.getAsJsonObject("display"));
                if(json.has("members"))for(var value:json.getAsJsonArray("members")){
                    var m=value.getAsJsonObject();keys(m,"id","template","entry");if(m.has("entry"))entry(m.getAsJsonObject("entry"));
                }
            }
            default -> throw new IllegalArgumentException("Unknown definition directory: " + directory);
        }
    }
    private static void entry(JsonObject json) {
        keys(json,"id","display","contributions","stacking","stacking_group");
        if(json.has("display"))display(json.getAsJsonObject("display"));
        if(json.has("contributions"))for(var value:json.getAsJsonArray("contributions")){
            var c=value.getAsJsonObject();
            switch(c.get("kind").getAsString()){
                case "DAMAGE" -> {keys(c,"id","kind","rule","source");rule(c.getAsJsonObject("rule"));}
                case "ATTRIBUTE" -> keys(c,"id","kind","attribute","amount","operation");
                case "BEHAVIOR","EFFECT","REGENERATION","ABILITY" -> keys(c,"id","kind","definition");
                case "SHIELD" -> keys(c,"id","kind","definition","amount","duration_ticks");
                default -> throw new IllegalArgumentException("Unknown contribution kind");
            }
        }
    }
    private static void display(JsonObject json) { keys(json, "name", "authored_summary", "flavor_text", "show_rule_breakdown"); }
    private static void rule(JsonObject json) {
        keys(json, "id", "role", "phase", "priority", "conditions", "operations", "stacking", "stacking_group");
        if (json.has("conditions")) for (var value : json.getAsJsonArray("conditions")) condition(value.getAsJsonObject());
        if (json.has("operations")) for (var value : json.getAsJsonArray("operations")) {
            var object = value.getAsJsonObject();
            var codec = DamageRuleOperationTypes.codec(NexusId.parse(object.get("type").getAsString()));
            typedKeys(object, codec.keys(JsonOps.INSTANCE).map(JsonElement::getAsString).toList());
        }
    }
    private static void condition(JsonObject json) {
        var type = NexusId.parse(json.get("type").getAsString());
        typedKeys(json, DamageRuleConditionTypes.codec(type).keys(JsonOps.INSTANCE).map(JsonElement::getAsString).toList());
        // Built-in composite children. Extension payload semantics belong to their registered codec.
        if (type.equals(NexusId.fromNamespaceAndPath("combatnexus", "all_of")) || type.equals(NexusId.fromNamespaceAndPath("combatnexus", "any_of")))
            for (var value : json.getAsJsonArray("conditions")) condition(value.getAsJsonObject());
        if (type.equals(NexusId.fromNamespaceAndPath("combatnexus", "not"))) condition(json.getAsJsonObject("condition"));
    }
    private static void typedKeys(JsonObject json, List<String> fields) {
        var allowed = new HashSet<>(fields); allowed.add("type");
        if (!allowed.containsAll(json.keySet())) throw new IllegalArgumentException("Unknown typed definition field: " + json.keySet());
    }
    private static void keys(JsonObject json, String... fields) {
        if (!Set.of(fields).containsAll(json.keySet())) throw new IllegalArgumentException("Unknown definition field: " + json.keySet());
    }
}
