package io.github.fatalitaya.combatnexus.presentation.platform.client;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.presentation.platform.EntityAttributeRows;
import io.github.fatalitaya.combatnexus.presentation.platform.network.CombatPresentationPayload;
import io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** One supplement per actual client entity instance; native facts are read from that same instance. */
public final class ClientEntityCombatMirrors {
    public record Snapshot(long incarnation, long revision, long receivedAt, JsonObject data) {
        public Snapshot { data = data.deepCopy(); }
        @Override public JsonObject data() { return data.deepCopy(); }
    }
    private record Mirror(long incarnation, long revision, long receivedAt, JsonObject data) {}
    private static final Map<LivingEntity, Mirror> mirrors = new WeakHashMap<>();
    private ClientEntityCombatMirrors() {}
    public static Optional<Snapshot> snapshot(LivingEntity entity) {
        if (entity.level() != Minecraft.getInstance().level || entity.isRemoved()) return Optional.empty();
        return Optional.ofNullable(mirrors.get(entity)).map(m -> new Snapshot(m.incarnation(), m.revision(), m.receivedAt(), m.data()));
    }
    public static void clear() { mirrors.clear(); }
    public static void remove(LivingEntity entity) { mirrors.remove(entity); }
    public static void accept(CombatPresentationPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null || !NativeIds.nexus(level.dimension().identifier()).equals(payload.dimension())
                || !payload.session().equals(CombatControlClient.session())
                || !(level.getEntity(payload.entityId()) instanceof LivingEntity entity) || !entity.getUUID().equals(payload.entity())) return;
        var previous = mirrors.get(entity);
        if (previous != null && payload.revision() <= previous.revision()) return;
        if (payload.removed()) { mirrors.remove(entity); return; }
        mirrors.put(entity, new Mirror(payload.incarnation(), payload.revision(), System.nanoTime(), JsonParser.parseString(payload.json()).getAsJsonObject()));
    }
    public static Optional<String> refusal(LivingEntity entity) {
        var mirror = mirrors.get(entity);
        return mirror != null && mirror.data().has("reason") ? Optional.of(mirror.data().get("reason").getAsString()) : Optional.empty();
    }
    /** Available for mobs, remote players and the local player through the same API. */
    public static JsonArray rows(LivingEntity entity) {
        if (entity.level() != Minecraft.getInstance().level || entity.isRemoved()) throw new IllegalArgumentException("Entity is not in the client world");
        var mirror = mirrors.get(entity); var data = mirror == null ? new JsonObject() : mirror.data();
        var rows = new JsonArray(); var health = new JsonObject();
        health.addProperty("health", entity.getHealth());
        if (data.has("absorption")) health.addProperty("absorption", data.get("absorption").getAsDouble());
        else if (entity instanceof net.minecraft.world.entity.player.Player) health.addProperty("absorption", entity.getAbsorptionAmount());
        rows.add(health);
        var attributes = new TreeMap<io.github.fatalitaya.combatnexus.api.id.NexusId, JsonArray>();
        for (var holder : Catalog.ATTRIBUTES) {
            if (!holder.value().isClientSyncable()) continue;
            var instance = entity.getAttribute(holder); if (instance == null) continue;
            var id = NativeIds.nexus(holder.unwrapKey().orElseThrow().identifier());
            var row = EntityAttributeRows.value(id, instance.getBaseValue(), entity.getAttributeValue(holder));
            if (data.has("effective") && data.getAsJsonObject("effective").has(id.toString()))
                data.getAsJsonObject("effective").getAsJsonObject(id.toString()).entrySet().forEach(entry -> row.add(entry.getKey(), entry.getValue().deepCopy()));
            var values = new JsonArray(); values.add(row);
            for (var modifier : instance.getModifiers()) values.add("  " + AttributeAccess.get().modifierId(modifier) + " : " + modifier.amount() + " (" + AttributeAccess.get().operation(modifier) + ")");
            attributes.put(id, values);
        }
        if (data.has("attributes")) data.getAsJsonObject("attributes").entrySet().forEach(entry -> {
            var captured = entry.getValue().getAsJsonObject(); var values = new JsonArray();
            values.add(captured.get("row").deepCopy()); values.addAll(captured.getAsJsonArray("sources").deepCopy());
            attributes.put(io.github.fatalitaya.combatnexus.api.id.NexusId.parse(entry.getKey()), values);
        });
        attributes.values().forEach(rows::addAll);
        if (data.has("shields")) for (var element : data.getAsJsonArray("shields")) {
            var shield = element.getAsJsonObject(); rows.add("Shield " + string(shield, "source", "unknown") + ": " + shield.get("balance").getAsDouble() + " / " + shield.get("capacity").getAsDouble());
        }
        if (data.has("effects")) for (var element : data.getAsJsonArray("effects")) {
            var effect = element.getAsJsonObject();
            rows.add("Effect " + string(effect, "definition", "unknown") + " [" + string(effect, "source", "unknown") + "] level " + effect.get("level").getAsInt()
                + " " + string(effect, "ownership", "unknown") + " " + effect.get("tags") + " dispellable=" + effect.get("dispellable").getAsBoolean()
                + (effect.has("expires") ? " ticks " + Math.max(0, effect.get("expires").getAsLong() - entity.level().getGameTime()) : ""));
        }
        if (data.has("behaviors")) for (var element : data.getAsJsonArray("behaviors")) {
            var behavior = element.getAsJsonObject(); rows.add("Behavior " + string(behavior, "effect", "unknown") + " [" + string(behavior, "source", "unknown")
                + "] protection=" + behavior.get("protection").getAsBoolean() + " types=" + behavior.get("types"));
        }
        if (data.has("reactions")) for (var element : data.getAsJsonArray("reactions")) {
            var reaction = element.getAsJsonObject(); rows.add("Reaction " + string(reaction, "reaction", "unknown") + " #" + reaction.get("operation").getAsLong()
                + " " + string(reaction, "source", "builtin") + " : " + string(reaction, "outcome", "unknown") + (reaction.has("child") ? " -> " + string(reaction, "child", "") : ""));
        }
        if(data.has("equipment"))for(var element:data.getAsJsonArray("equipment")){
            var entry=element.getAsJsonObject();rows.add(string(entry,"slot","")+" / "+string(entry,"definition","")+" : "+state(string(entry,"state","")));
        }
        if(data.has("sources"))for(var element:data.getAsJsonArray("sources")){
            var source=element.getAsJsonObject();rows.add("  "+string(source,"path","")+" : "+state(string(source,"state",""))+(source.get("consumed").getAsBoolean()?" ("+state("CONSUMED")+")":""));
        }
        if (data.has("descriptions")) rows.addAll(data.getAsJsonArray("descriptions").deepCopy());
        if (mirror == null) { var loading = new JsonObject(); loading.addProperty("message", "gui.combatnexus.panel.loading"); rows.add(loading); }
        return rows;
    }
    private static String state(String value){return net.minecraft.network.chat.Component.translatable("gui.combatnexus.equipment."+value.toLowerCase(java.util.Locale.ROOT)).getString();}
    private static String string(JsonObject object, String key, String fallback) { return object.has(key) ? object.get(key).getAsString() : fallback; }
    private static final class Catalog {
        static final java.util.List<net.minecraft.core.Holder.Reference<net.minecraft.world.entity.ai.attributes.Attribute>> ATTRIBUTES = BuiltInRegistries.ATTRIBUTE.listElements().toList();
    }
}
