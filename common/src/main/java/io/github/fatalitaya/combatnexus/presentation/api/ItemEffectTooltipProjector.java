package io.github.fatalitaya.combatnexus.presentation.api;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.function.BiFunction;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts;
import java.util.*;

/** Reads adopted definitions only; never executes effects, predicates or random rolls. */
public final class ItemEffectTooltipProjector {
    private ItemEffectTooltipProjector() {}
    public static List<TooltipBlock> project(ItemEffectObservation observed, BiFunction<NexusId, CombatItemBindingSpec.Kind, Optional<TooltipBlock>> definitions, boolean managedProtection, double ratingPerScore, EnchantmentValueEvaluation values, java.util.function.Supplier<List<TooltipBlock>> extensions, PresentationBudget budget) {
        var result = new ArrayList<TooltipBlock>();
        for (var entry : observed.enchantments()) {
            int enchantmentLevel = entry.level();
            var encoded = entry.definition();
            var lines = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            var effects = encoded.getAsJsonObject("effects");
            if (effects == null) continue;
            for (var field : effects.entrySet()) {
                String kind = switch (field.getKey()) {
                    case "minecraft:damage" -> "damage";
                    case "minecraft:damage_protection" -> "protection";
                    case "minecraft:armor_effectiveness" -> "armor";
                    case "minecraft:smash_damage_per_fallen_block" -> "smash";
                    case "minecraft:knockback" -> "knockback";
                    default -> null;
                };
                // Vanilla already presents attribute components. Unsupported effects retain their native tooltip.
                if (field.getKey().equals("minecraft:attributes")) continue;
                if (kind == null) continue;
                if (!field.getValue().isJsonArray()) throw new IllegalArgumentException("Invalid enchantment effects");
                for (var raw : field.getValue().getAsJsonArray()) {
                    budget.entry(1);
                    var effect = raw.getAsJsonObject();
                    var action = valueEffect(effect.getAsJsonObject("effect"), kind, enchantmentLevel, 0, values);
                    var operation = effect.getAsJsonObject("effect");
                    if (kind.equals("protection") && managedProtection && operation.get("type").getAsString().equals("minecraft:add")) {
                        double score = values.evaluate(operation.get("value"), enchantmentLevel);
                        action = GeneratedTexts.join(List.of(action, text("converted_resistance", TooltipNumbers.number(score * ratingPerScore))), " ");
                    }
                    lines.add(effect.has("requirements") ? text("conditional", condition(effect.get("requirements"), 0), action) : action);
                }
                if (kind.equals("protection")) details.add(text(managedProtection ? "protection_managed" : "protection_native"));
                if (kind.equals("armor")) details.add(text("armor_detail"));
            }
            if (lines.isEmpty()) continue;
            if (encoded.has("slots")) lines.add(0, text("equipped", join(encoded.getAsJsonArray("slots").asList().stream()
                    .map(value -> text("slot." + value.getAsString())).toList(), ", ")));
            result.add(new TooltipBlock(entry.title(), lines, details));
        }
        result.addAll(extensions.get());
        return List.copyOf(result);
    }
    private static DisplayText valueEffect(JsonObject effect, String kind, int level, int depth, EnchantmentValueEvaluation values) {
        if (depth > 16 || effect == null || !effect.has("type")) throw new IllegalArgumentException("Unsupported enchantment value effect");
        String type = effect.get("type").getAsString();
        if (type.equals("minecraft:all_of")) return join(effect.getAsJsonArray("effects").asList().stream()
                .map(child -> valueEffect(child.getAsJsonObject(), kind, level, depth + 1, values)).toList(), "; ");
        String operation = switch (type) {
            case "minecraft:add" -> "add";
            case "minecraft:multiply" -> "multiply";
            case "minecraft:set" -> "set";
            default -> null;
        };
        String field = type.equals("minecraft:multiply") ? "factor" : "value";
        if (operation == null || !effect.has(field)) throw new IllegalArgumentException("Unsupported enchantment value effect");
        float value = values.evaluate(effect.get(field), level);
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite enchantment value");
        return text("value." + operation, text("quantity." + kind), TooltipNumbers.number(value));
    }
    private static DisplayText condition(JsonElement raw, int depth) {
        if (depth > 16 || !raw.isJsonObject()) throw new IllegalArgumentException("Unsupported enchantment condition");
        var object = raw.getAsJsonObject();
        if (!object.has("condition")) throw new IllegalArgumentException("Unsupported enchantment condition");
        return switch (object.get("condition").getAsString()) {
            case "minecraft:all_of", "minecraft:any_of" -> text(object.get("condition").getAsString().endsWith("all_of") ? "all" : "any",
                    join(object.getAsJsonArray("terms").asList().stream().map(term -> condition(term, depth + 1)).toList(), "; "));
            case "minecraft:inverted" -> text("not", condition(object.get("term"), depth + 1));
            case "minecraft:entity_properties" -> {
                var predicate = object.getAsJsonObject("predicate");
                if (predicate == null || predicate.size() != 1 || !predicate.has("type") || !predicate.get("type").isJsonPrimitive()) throw new IllegalArgumentException("Unsupported enchantment condition");
                yield text("entity_condition", text("entity." + object.get("entity").getAsString()), identifier(predicate.get("type").getAsString(), "entity"));
            }
            case "minecraft:damage_source_properties" -> {
                var predicate = object.getAsJsonObject("predicate");
                if (predicate == null) throw new IllegalArgumentException("Unsupported enchantment condition");
                var parts = new ArrayList<DisplayText>();
                for (var field : predicate.entrySet()) {
                    if (field.getKey().equals("tags")) for (var tag : field.getValue().getAsJsonArray()) {
                        var value = tag.getAsJsonObject();
                        parts.add(text(value.get("expected").getAsBoolean() ? "damage_tag" : "not_damage_tag", identifier("#" + value.get("id").getAsString(), "damage_type")));
                    } else if (field.getKey().equals("is_direct")) parts.add(text(field.getValue().getAsBoolean() ? "direct" : "indirect"));
                    else throw new IllegalArgumentException("Unsupported damage source predicate: " + field.getKey());
                }
                if (parts.isEmpty()) throw new IllegalArgumentException("Empty damage source predicate");
                yield text("all", join(parts, "; "));
            }
            default -> throw new IllegalArgumentException("Unsupported enchantment condition");
        };
    }
    private static DisplayText identifier(String value, String kind) {
        boolean tag = value.startsWith("#");
        String id = tag ? value.substring(1) : value;
        return GeneratedTexts.translatableWithFallback((tag ? "tag." + (kind.equals("entity") ? "entity_type" : kind) : kind) + "." + id.replace(':', '.').replace('/', '.'), value);
    }
    private static DisplayText join(List<DisplayText> values, String separator) {
        return GeneratedTexts.join(values, separator);
    }
    private static DisplayText text(String key, Object... args) { return GeneratedTexts.translatable("tooltip.combatnexus.item_effect." + key, args); }
}
