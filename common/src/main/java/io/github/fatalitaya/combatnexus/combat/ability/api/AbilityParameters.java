package io.github.fatalitaya.combatnexus.combat.ability.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.codec.CodecBudgets;
import io.github.fatalitaya.combatnexus.api.id.*;
import java.util.*;

/** Detached, bounded executor parameters and continuation values. */
public record AbilityParameters(Map<String, Double> numbers, Map<String, NexusId> ids) {
    public static final AbilityParameters EMPTY = new AbilityParameters(Map.of(), Map.of());
    private static final Codec<String> KEY = CodecBudgets.boundedString(64, "Ability parameter key");
    public static final Codec<AbilityParameters> CODEC = CodecBudgets.guardRawStructure(RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(KEY, Codec.DOUBLE).optionalFieldOf("numbers", Map.of()).forGetter(AbilityParameters::numbers),
        Codec.unboundedMap(KEY, NexusIdCodecs.CODEC).optionalFieldOf("ids", Map.of()).forGetter(AbilityParameters::ids)
    ).apply(i, AbilityParameters::new)), "Ability parameters", 4, 200);
    public AbilityParameters {
        Objects.requireNonNull(numbers); Objects.requireNonNull(ids);
        if (numbers.size() + ids.size() > 32) throw new IllegalArgumentException("Ability parameter capacity exceeded");
        numbers.forEach((key,value) -> { requireKey(key); if (value == null || !Double.isFinite(value)) throw new IllegalArgumentException("Ability number must be finite"); });
        ids.forEach((key,value) -> { requireKey(key); Objects.requireNonNull(value); });
        if (numbers.keySet().stream().anyMatch(ids::containsKey)) throw new IllegalArgumentException("Ambiguous ability parameter");
        numbers = Map.copyOf(numbers); ids = Map.copyOf(ids);
    }
    private static void requireKey(String key) {
        if (key == null || key.isBlank() || key.codePointCount(0, key.length()) > 64) throw new IllegalArgumentException("Invalid ability parameter key");
    }
    public double number(String key) { return Objects.requireNonNull(numbers.get(key), "Missing ability number: " + key); }
    public NexusId id(String key) { return Objects.requireNonNull(ids.get(key), "Missing ability ID: " + key); }
}
