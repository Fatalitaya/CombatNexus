package io.github.fatalitaya.combatnexus.combat.internal.definition;
import io.github.fatalitaya.combatnexus.combat.api.DomainDefinitions;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorDefinition;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationDefinition;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldDefinition;
import io.github.fatalitaya.combatnexus.effect.api.EffectDefinition;
import io.github.fatalitaya.combatnexus.config.SettingsJson;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.config.DefinitionResources;
import java.util.*;
import java.util.function.Function;
/** Strict domain readers share the aggregate reload's failure/publication boundary. */
final class DomainDefinitionResources {
    private DomainDefinitionResources() {}
    static DomainDefinitions read(DefinitionResources manager, Map<NexusId, BehaviorDefinition> behaviors) {
        var shields = read(manager, "combatnexus_shields", 256, ShieldDefinition::decode);
        for (var shield : shields.values()) {
            shield.damageTypes().forEach(id -> requireResource(manager, id, "damage_type/"));
            shield.damageTypeTags().forEach(id -> requireResource(manager, id, "tags/damage_type/"));
        }
        return new DomainDefinitions(read(manager, "combatnexus_healing_rules", 64, HealingRuleDefinition::decode),
                read(manager, "combatnexus_regeneration", 256, RegenerationDefinition::decode), shields,
                read(manager, "combatnexus_effects", 256, json -> EffectDefinition.decode(json, behaviors)),
                readIdentified(manager, "combatnexus_abilities", 256, io.github.fatalitaya.combatnexus.combat.ability.api.AbilityDefinition::decode));
    }
    private static void requireResource(DefinitionResources manager, NexusId id, String prefix) {
        if (manager.contains(NexusId.fromNamespaceAndPath(id.getNamespace(), prefix + id.getPath() + ".json")) == false)
            throw new IllegalArgumentException("Unknown shield filter resource: " + prefix + id);
    }
    private static <T> Map<NexusId, T> read(DefinitionResources manager, String directory, int maximum,
            Function<com.google.gson.JsonObject, T> decode) {
        return readIdentified(manager, directory, maximum, (id, json) -> decode.apply(json));
    }
    private static <T> Map<NexusId, T> readIdentified(DefinitionResources manager, String directory, int maximum,
            java.util.function.BiFunction<NexusId, com.google.gson.JsonObject, T> decode) {
        String directoryName = directory;
        var files = manager.json(directoryName);
        if (files.size() > maximum) throw new IllegalArgumentException("Resource budget exceeded: " + directory);
        var result = new LinkedHashMap<NexusId, T>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (var reader = entry.getValue().openAsReader()) {
                var id = DefinitionResources.definitionId(directoryName, entry.getKey());
                if (id.toString().length() > 256) throw new IllegalArgumentException("Definition ID too long");
                result.put(id, decode.apply(id, SettingsJson.read(reader)));
            } catch (java.io.IOException | RuntimeException failure) {
                throw new IllegalArgumentException("Invalid domain definition: " + entry.getKey(), failure);
            }
        });
        return Map.copyOf(result);
    }
}
