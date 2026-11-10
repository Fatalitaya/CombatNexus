package io.github.fatalitaya.combatnexus.presentation.api;

import com.google.gson.*;

/** Encodes supplemental facts; layout and native entity values remain client-owned. */
public final class CombatPanelProjector {
    private CombatPanelProjector() {}
    public static JsonObject refused(String reason) {
        var object = new JsonObject(); object.addProperty("status", "refused"); object.addProperty("reason", reason); return object;
    }
    public static JsonObject supplement(CombatPanelSnapshot value, long now) {
        var json = new JsonObject(); var budget = new PresentationBudget(256, 24000);
        var shields = new JsonArray();
        for (var shield : value.shields()) {
            var row = new JsonObject(); row.addProperty("source", shield.source().id().toString());
            row.addProperty("balance", shield.balance()); row.addProperty("capacity", shield.definition().capacity());
            add(budget, shields, row);
        }
        json.add("shields", shields);
        var effects = new JsonArray();
        for (var effect : value.effects()) {
            var row = new JsonObject(); row.addProperty("definition", effect.definition().toString());
            effect.source().ifPresent(source -> row.addProperty("source", source.toString()));
            effect.sourceInstance().ifPresent(instance -> row.addProperty("instance", instance.toString()));
            effect.owner().ifPresent(owner -> row.addProperty("owner", owner.toString()));
            row.addProperty("level", effect.level()); row.addProperty("ownership", effect.ownership().name());
            row.addProperty("completeness", effect.completeness().name());
            var tags = new JsonArray(); effect.tags().stream().sorted().forEach(tag -> tags.add(tag.toString())); row.add("tags", tags);
            row.addProperty("dispellable", effect.operations().contains(io.github.fatalitaya.combatnexus.effect.api.EffectView.Operation.DISPEL));
            effect.remainingTicks().ifPresent(remaining -> row.addProperty("expires", Math.addExact(now, remaining)));
            add(budget, effects, row);
        }
        json.add("effects", effects);
        var behaviors = new JsonArray();
        for (var behavior : value.behaviors()) {
            var row = new JsonObject();
            row.addProperty("effect", behavior.effect().toString()); row.addProperty("source", behavior.source().toString());
            row.addProperty("installation", behavior.installation()); row.addProperty("protection", behavior.protectionAvailable());
            var types = new JsonArray(); behavior.componentTypes().forEach(type -> types.add(type.toString())); row.add("types", types);
            add(budget, behaviors, row);
        }
        json.add("behaviors", behaviors);
        var reactions = new JsonArray();
        for (var reaction : value.reactions()) {
            var row = new JsonObject(); row.addProperty("reaction", reaction.reaction().toString()); row.addProperty("operation", reaction.operation());
            reaction.source().ifPresent(source -> row.addProperty("source", source.toString()));
            row.addProperty("outcome", reaction.outcome()); reaction.childRequest().ifPresent(child -> row.addProperty("child", child));
            add(budget, reactions, row);
        }
        json.add("reactions", reactions);
        var descriptions = new JsonArray();
        for (var source : value.sourceDescriptions()) {
            var row = new JsonObject(); row.addProperty("source", source.source().toString());
            row.add("display", DisplayText.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, source.description()).getOrThrow()); add(budget, descriptions, row);
        }
        json.add("descriptions", descriptions);
        var equipment=new JsonArray();
        for(var e:value.equipment()){var row=new JsonObject();row.addProperty("installation",e.installation().toString());row.addProperty("slot",e.slot().getName());row.addProperty("binding",e.binding().toString());row.addProperty("definition",e.definition().toString());row.addProperty("state",e.selected()?"ACTIVE":"COMPETING");add(budget,equipment,row);}
        json.add("equipment",equipment);
        var sources=new JsonArray();
        for(var s:value.sources()){var row=new JsonObject();row.addProperty("instance",s.source().instance().toString());row.addProperty("path",s.source().slot());row.addProperty("definition",s.definition().toString());row.addProperty("state",s.pending()?"PENDING":s.suspended()?"SUSPENDED":s.consumed()?"CONSUMED":"ACTIVE");row.addProperty("consumed",s.consumed());add(budget,sources,row);}
        json.add("sources",sources);
        return json;
    }
    private static void add(PresentationBudget budget, JsonArray rows, JsonElement row) {
        budget.entry(1); budget.encoded(row.toString()); rows.add(row);
    }
}
