package io.github.fatalitaya.combatnexus.combat.ability.internal;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.ability.api.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import java.util.*;
import java.lang.reflect.Type;

/** Explicit codecs for polymorphic values and detached command evidence. */
public final class AbilityStateAdapters {
    private AbilityStateAdapters() {}
    public static GsonBuilder register(GsonBuilder builder){return builder
        .registerTypeAdapter(io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample.class,new SampleAdapter())
        .registerTypeAdapter(AbilityDefinition.class,new DefinitionAdapter())
        .registerTypeAdapter(AbilityTarget.class,new TargetAdapter())
        .registerTypeAdapter(AbilitySnapshot.CommandEvidence.class,new EvidenceAdapter());}
    private static final class SampleAdapter implements JsonSerializer<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample>,JsonDeserializer<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample>{
        public JsonElement serialize(io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample value,Type type,JsonSerializationContext context){
            var json=new JsonObject();json.addProperty("policy",value.policy().toString());json.addProperty("decision",value.decision().toString());json.addProperty("value",value.value());value.target().ifPresent(target->json.addProperty("target",target.toString()));return json;
        }
        public io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample deserialize(JsonElement value,Type type,JsonDeserializationContext context){
            var json=value.getAsJsonObject();return new io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample(NexusId.parse(json.get("policy").getAsString()),NexusId.parse(json.get("decision").getAsString()),json.has("target")?Optional.of(UUID.fromString(json.get("target").getAsString())):Optional.empty(),json.get("value").getAsFloat());
        }
    }
    private static final class DefinitionAdapter implements JsonSerializer<AbilityDefinition>,JsonDeserializer<AbilityDefinition>{
        public JsonElement serialize(AbilityDefinition value,Type type,JsonSerializationContext context){return value.encode();}
        public AbilityDefinition deserialize(JsonElement value,Type type,JsonDeserializationContext context){return AbilityDefinition.decode(NexusId.parse(value.getAsJsonObject().get("id").getAsString()),value.getAsJsonObject());}
    }
    private static final class TargetAdapter implements JsonSerializer<AbilityTarget>,JsonDeserializer<AbilityTarget>{
        public JsonElement serialize(AbilityTarget value,Type type,JsonSerializationContext context){return AbilityTarget.CODEC.encodeStart(JsonOps.INSTANCE,value).getOrThrow();}
        public AbilityTarget deserialize(JsonElement value,Type type,JsonDeserializationContext context){return AbilityTarget.CODEC.parse(JsonOps.INSTANCE,value).getOrThrow();}
    }
    private static final class EvidenceAdapter implements JsonSerializer<AbilitySnapshot.CommandEvidence>,JsonDeserializer<AbilitySnapshot.CommandEvidence>{
        public JsonElement serialize(AbilitySnapshot.CommandEvidence value,Type type,JsonSerializationContext context){
            var json=new JsonObject();json.addProperty("applied",value.applied());json.addProperty("completed",value.journal().applicationCompleted());
            value.rejection().ifPresent(r->json.add("rejection",context.serialize(r)));
            value.context().ifPresent(c->{
                var j=new JsonObject();j.addProperty("request",c.request().toString());j.addProperty("target",c.target().toString());j.addProperty("operation",c.operation().name());
                c.action().ifPresent(a->j.add("action",context.serialize(a)));c.source().ifPresent(s->j.add("source",context.serialize(s)));c.identity().ifPresent(i->j.add("identity",context.serialize(i)));json.add("context",j);
            });
            var writes=new JsonArray();for(var entry:value.journal().entries()){
                var j=new JsonObject();j.addProperty("resource",entry.resource().name());entry.source().ifPresent(s->j.addProperty("source",s.toString()));
                entry.before().ifPresent(n->j.addProperty("before",n));entry.after().ifPresent(n->j.addProperty("after",n));j.addProperty("confirmed",entry.confirmedAmount());
                j.addProperty("observation",entry.observation().name());j.addProperty("completed",entry.completed());writes.add(j);
            }json.add("writes",writes);return json;
        }
        public AbilitySnapshot.CommandEvidence deserialize(JsonElement value,Type type,JsonDeserializationContext context){
            var json=value.getAsJsonObject();
            if(!Set.of("applied","completed","rejection","context","writes").containsAll(json.keySet()))throw new IllegalArgumentException("Unknown saved ability evidence field");
            Optional<CommandContext> command=Optional.empty();if(json.has("context")){
                var c=json.getAsJsonObject("context");command=Optional.of(new CommandContext(UUID.fromString(c.get("request").getAsString()),UUID.fromString(c.get("target").getAsString()),CombatOperation.valueOf(c.get("operation").getAsString()),
                    c.has("source")?Optional.of(context.deserialize(c.get("source"),CombatSource.class)):Optional.empty(),c.has("identity")?Optional.of(context.deserialize(c.get("identity"),CommandContext.Identity.class)):Optional.empty(),c.has("action")?Optional.of(context.deserialize(c.get("action"),io.github.fatalitaya.combatnexus.combat.api.action.CombatAction.class)):Optional.empty()));
            }
            var writes=new ArrayList<ApplicationJournal.Entry>();for(var element:json.getAsJsonArray("writes")){
                if(writes.size()>=1024)throw new IllegalArgumentException("Saved ability journal budget exceeded");var j=element.getAsJsonObject();
                writes.add(new ApplicationJournal.Entry(ApplicationJournal.Resource.valueOf(j.get("resource").getAsString()),j.has("source")?Optional.of(NexusId.parse(j.get("source").getAsString())):Optional.empty(),
                    j.has("before")?OptionalDouble.of(j.get("before").getAsDouble()):OptionalDouble.empty(),j.has("after")?OptionalDouble.of(j.get("after").getAsDouble()):OptionalDouble.empty(),j.get("confirmed").getAsDouble(),
                    ApplicationJournal.Observation.valueOf(j.get("observation").getAsString()),j.get("completed").getAsBoolean()));
            }
            return new AbilitySnapshot.CommandEvidence(command,json.get("applied").getAsBoolean(),new ApplicationJournal(writes,json.get("completed").getAsBoolean()),
                json.has("rejection")?Optional.of(context.deserialize(json.get("rejection"),CommandRejection.class)):Optional.empty());
        }
    }
}
