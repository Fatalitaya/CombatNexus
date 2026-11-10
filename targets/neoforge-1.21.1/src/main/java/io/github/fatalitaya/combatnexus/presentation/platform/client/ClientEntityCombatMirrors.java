package io.github.fatalitaya.combatnexus.presentation.platform.client;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.presentation.platform.network.CombatPresentationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Complete server-owned panel data for the current screen subscription. */
public final class ClientEntityCombatMirrors {
    public record Snapshot(UUID subscription, long generation, long incarnation, long revision, long receivedAt, JsonObject data) {
        public Snapshot { data = data.deepCopy(); }
        @Override public JsonObject data() { return data.deepCopy(); }
    }
    private record Mirror(long generation, long incarnation, long revision, long receivedAt, JsonObject data) {}
    private static final Map<LivingEntity, Mirror> mirrors = new WeakHashMap<>();
    private static LivingEntity subscribed;
    private static UUID subscription, session;
    private ClientEntityCombatMirrors() {}
    public static void subscribe(LivingEntity entity) {
        UUID current = CombatControlClient.session();
        if (current == null || entity == subscribed && current.equals(session)) return;
        clear();
        subscribed = entity; subscription = UUID.randomUUID(); session = current;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new io.github.fatalitaya.combatnexus.presentation.platform.network.CombatPanelSubscriptionPayload(subscription, session, entity.getId(), entity.getUUID(), true));
    }

    public static Optional<Snapshot> snapshot(LivingEntity entity) {
        if (entity.level() != Minecraft.getInstance().level || entity.isRemoved()) return Optional.empty();
        return Optional.ofNullable(mirrors.get(entity)).map(m -> new Snapshot(subscription, m.generation(), m.incarnation(), m.revision(), m.receivedAt(), m.data()));
    }
    public static void clear() {
        if (subscribed != null && Minecraft.getInstance().getConnection() != null && session.equals(CombatControlClient.session()))
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new io.github.fatalitaya.combatnexus.presentation.platform.network.CombatPanelSubscriptionPayload(subscription, session, subscribed.getId(), subscribed.getUUID(), false));
        subscribed = null; subscription = null; session = null; mirrors.clear();
    }
    public static void remove(LivingEntity entity) { if (entity == subscribed) clear(); }
    public static void accept(CombatPresentationPayload payload) {
        var level = Minecraft.getInstance().level;
        if (payload.generation() != CombatPresentationClient.generation() || subscribed == null || !payload.subscription().equals(subscription) || !payload.entity().equals(subscribed.getUUID()) || level == null || !NativeIds.nexus(level.dimension().location()).equals(payload.dimension())
                || !payload.session().equals(CombatControlClient.session())
                || !(level.getEntity(payload.entityId()) instanceof LivingEntity entity) || entity != subscribed || !entity.getUUID().equals(payload.entity())) return;
        var previous = mirrors.get(entity);
        if (previous != null && payload.revision() <= previous.revision()) return;
        if (payload.removed()) { clear(); return; }
        mirrors.put(entity, new Mirror(payload.generation(), payload.incarnation(), payload.revision(), System.nanoTime(), JsonParser.parseString(payload.json()).getAsJsonObject()));
    }
    public static Optional<String> refusal(LivingEntity entity) {
        var mirror = mirrors.get(entity);
        return mirror != null && mirror.data().has("reason") ? Optional.of(mirror.data().get("reason").getAsString()) : Optional.empty();
    }
    /** Available for mobs, remote players and the local player through the same API. */
    public static JsonArray rows(LivingEntity entity) {
        if (entity.level() != Minecraft.getInstance().level || entity.isRemoved()) throw new IllegalArgumentException("Entity is not in the client world");
        var mirror = mirrors.get(entity); var data = mirror == null ? new JsonObject() : mirror.data();
        var rows = new JsonArray();
        if (mirror == null) {
            var loading = new JsonObject(); loading.addProperty("message", "gui.combatnexus.panel.loading"); rows.add(loading); return rows;
        }
        if (data.has("reason")) { rows.add("Panel rejected: " + data.get("reason").getAsString()); return rows; }
        var health = new JsonObject(); health.add("health", data.get("health").deepCopy()); health.add("absorption", data.get("absorption").deepCopy()); rows.add(health);
        var attributes = new TreeMap<io.github.fatalitaya.combatnexus.api.id.NexusId, JsonArray>();
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
                + (effect.has("expires") ? " ticks " + Math.max(0, effect.get("expires").getAsLong() - entity.level().getGameTime()) : effect.has("remaining") ? " ticks " + effect.get("remaining").getAsLong() : ""));
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
        return rows;
    }
    private static String state(String value){return net.minecraft.network.chat.Component.translatable("gui.combatnexus.equipment."+value.toLowerCase(java.util.Locale.ROOT)).getString();}
    private static String string(JsonObject object, String key, String fallback) { return object.has(key) ? object.get(key).getAsString() : fallback; }
}
