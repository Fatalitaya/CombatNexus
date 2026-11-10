package io.github.fatalitaya.combatnexus.combat.ability.api;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.codec.CodecBudgets;
import io.github.fatalitaya.combatnexus.api.id.*;
import java.util.*;

/** Generic ability content, without learning, preparation or tactical action costs. */
public record AbilityDefinition(NexusId id, String title, String description, Set<AbilityTarget.Kind> targets,
        NexusId executor, AbilityParameters parameters, int cooldownTicks, Optional<NexusId> sharedCooldown, AbilityTrigger trigger) {
    private static Set<AbilityTarget.Kind> targetSet(List<AbilityTarget.Kind> values) {
        if (new HashSet<>(values).size() != values.size()) throw new IllegalArgumentException("Duplicate ability target kind");
        return Set.copyOf(values);
    }
    private static final Codec<AbilityDefinition> RAW_CODEC = RecordCodecBuilder.create(i -> i.group(
        NexusIdCodecs.CODEC.fieldOf("id").forGetter(AbilityDefinition::id),
        CodecBudgets.boundedString(256, "Ability title").optionalFieldOf("title", "").forGetter(AbilityDefinition::title),
        CodecBudgets.boundedString(2048, "Ability description").optionalFieldOf("description", "").forGetter(AbilityDefinition::description),
        CodecBudgets.boundedList(Codec.STRING.xmap(AbilityTarget.Kind::valueOf, Enum::name), 4, "Ability target contract")
            .xmap(AbilityDefinition::targetSet, v -> v.stream().sorted().toList()).fieldOf("targets").forGetter(AbilityDefinition::targets),
        NexusIdCodecs.CODEC.fieldOf("executor").forGetter(AbilityDefinition::executor),
        AbilityParameters.CODEC.optionalFieldOf("parameters", AbilityParameters.EMPTY).forGetter(AbilityDefinition::parameters),
        Codec.INT.optionalFieldOf("cooldown_ticks", 0).forGetter(AbilityDefinition::cooldownTicks),
        NexusIdCodecs.CODEC.optionalFieldOf("shared_cooldown").forGetter(AbilityDefinition::sharedCooldown),
        AbilityTrigger.CODEC.optionalFieldOf("trigger", AbilityTrigger.ACTIVE).forGetter(AbilityDefinition::trigger)
    ).apply(i, AbilityDefinition::new));
    public static final Codec<AbilityDefinition> CODEC = CodecBudgets.guardRawStructure(new Codec<>() {
        public <T> DataResult<com.mojang.datafixers.util.Pair<AbilityDefinition,T>> decode(DynamicOps<T> ops,T input) {
            try { validateFields(ops.convertTo(JsonOps.INSTANCE,input).getAsJsonObject()); return RAW_CODEC.decode(ops,input); }
            catch (IllegalArgumentException | IllegalStateException failure) { return DataResult.error(()->"Invalid ability definition: "+failure.getMessage()); }
        }
        public <T> DataResult<T> encode(AbilityDefinition value,DynamicOps<T> ops,T prefix) { return RAW_CODEC.encode(value,ops,prefix); }
    }, "Ability definition", 8, 400);
    public AbilityDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(title); Objects.requireNonNull(description);
        Objects.requireNonNull(executor); Objects.requireNonNull(parameters); Objects.requireNonNull(sharedCooldown); Objects.requireNonNull(trigger);
        targets = Set.copyOf(targets);
        if (targets.isEmpty() || cooldownTicks < 0 || cooldownTicks > 1200000 || title.codePointCount(0,title.length()) > 256
                || description.codePointCount(0,description.length()) > 2048) throw new IllegalArgumentException("Invalid ability definition");
        if (trigger.kind() != AbilityTrigger.Kind.ACTIVE && !targets.equals(Set.of(AbilityTarget.Kind.SELF)))
            throw new IllegalArgumentException("Automatic ability requires SELF target");
    }
    public static AbilityDefinition decode(NexusId id, JsonObject json) {
        validateFields(json);
        var complete = json.deepCopy();
        if (complete.has("id") && !id.toString().equals(complete.get("id").getAsString())) throw new IllegalArgumentException("Ability ID differs from resource path");
        complete.addProperty("id", id.toString());
        return CODEC.parse(JsonOps.INSTANCE, complete).getOrThrow();
    }
    private static void validateFields(JsonObject json) {
        keys(json, "id", "title", "description", "targets", "executor", "parameters", "cooldown_ticks", "shared_cooldown", "trigger");
        if (json.has("parameters")) keys(json.getAsJsonObject("parameters"), "numbers", "ids");
        if (json.has("trigger")) {
            var trigger = json.getAsJsonObject("trigger"); keys(trigger, "kind", "limit", "interval_ticks", "first_delay_ticks");
            if (trigger.has("limit")) keys(trigger.getAsJsonObject("limit"), "scope", "maximum");
        }
    }
    public JsonObject encode() { return CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow().getAsJsonObject(); }
    private static void keys(JsonObject json, String... allowed) {
        if (!Set.of(allowed).containsAll(json.keySet())) throw new IllegalArgumentException("Unknown ability definition field: " + json.keySet());
    }
}
