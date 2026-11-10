package io.github.fatalitaya.combatnexus.combat.state.internal;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.state.api.*;
/** Entity-owned values. Native entity lifecycle invokes the declared boundaries. */
public final class RuntimeStateStore {
    private record Key(NexusId type,Optional<UUID> scope) {}
    private Map<Key,Stored<?>> values=new LinkedHashMap<>();
    private record Stored<T>(StateKey<T> type,T value) {
        T detached() { return Objects.requireNonNull(type.copy().apply(value), "State copy"); }
        JsonElement encoded() { return type.codec().encodeStart(JsonOps.INSTANCE,detached()).getOrThrow(); }
    }
    @SuppressWarnings("unchecked")
    private static <T> Stored<T> typed(Stored<?> stored, StateKey<T> type) {
        if(stored.type()!=type)throw new IllegalArgumentException("State definition identity mismatch");
        return (Stored<T>)stored;
    }
    private static <T> Stored<T> prepare(StateKey<T> type,T value) {
        T owned=Objects.requireNonNull(type.copy().apply(value), "State copy");
        var stored=new Stored<>(type,owned);
        if(stored.encoded().toString().length()>16384)throw new IllegalArgumentException("Runtime state value capacity");
        return stored;
    }
    private static <T> Stored<T> decode(StateKey<T> type,JsonElement value) {
        return prepare(type,type.codec().parse(JsonOps.INSTANCE,value).getOrThrow());
    }
    public <T> Optional<T> read(StateAddress<T> address) {
        RuntimeStateTypes.require(address.key());
        try(var read=io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
            return Optional.ofNullable(values.get(new Key(address.key().id(),address.scope()))).map(stored->typed(stored,address.key()).detached());
        }
    }
    public <T> T write(StateAddress<T> address,T value) {
        RuntimeStateTypes.require(address.key());var key=new Key(address.key().id(),address.scope());
        if(!values.containsKey(key)&&values.size()>=64)throw new IllegalStateException("Runtime state capacity");
        Stored<T> stored;T detached;
        try(var read=io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
            stored=prepare(address.key(),value);
            detached=stored.detached();
        }
        values.put(key,stored);return detached;
    }

    public boolean remove(StateAddress<?> address) { RuntimeStateTypes.require(address.key());return values.remove(new Key(address.key().id(),address.scope()))!=null; }
    public void clear(StateKey.Boundary boundary,Optional<UUID> scope) {
        values.keySet().removeIf(k->RuntimeStateTypes.get(k.type()).clearOn().contains(boundary)&&(scope.isEmpty()||k.scope().equals(scope)));
    }
    public String encode(boolean persistentOnly) {
        try(var read=io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
        var json=new JsonArray();values.forEach((key,value)->{
            if(persistentOnly&&!RuntimeStateTypes.get(key.type()).persistent())return;
            var entry=new JsonObject();entry.addProperty("type",key.type().toString());key.scope().ifPresent(id->entry.addProperty("scope",id.toString()));entry.add("value",value.encoded());json.add(entry);
        });return json.toString();
        }
    }
    public void restore(String encoded) {
        if(encoded.length()>1048576)throw new IllegalArgumentException("Runtime state save capacity");
        var restored=new LinkedHashMap<Key,Stored<?>>();
        for(var element:JsonParser.parseString(encoded).getAsJsonArray()) {
            var json=element.getAsJsonObject();var id=NexusId.parse(json.get("type").getAsString());var type=RuntimeStateTypes.get(id);
            var scope=json.has("scope")?Optional.of(UUID.fromString(json.get("scope").getAsString())):Optional.<UUID>empty();
            if((type.scope()==StateKey.Scope.ENTITY)!=scope.isEmpty())throw new IllegalArgumentException("Restored state scope mismatch");
            if(json.get("value").toString().length()>16384)throw new IllegalArgumentException("Runtime state value capacity");
            Stored<?> value;
            try(var read=io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) { value=decode(type,json.get("value")); }
            if(restored.putIfAbsent(new Key(id,scope),value)!=null||restored.size()>64)throw new IllegalArgumentException("Restored runtime state capacity/duplicate");
        }
        values=restored;
    }
}
