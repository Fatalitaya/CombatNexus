package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorDefinition;
import io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage;
import io.github.fatalitaya.combatnexus.effect.api.ScheduledEnvironmentDamage;
import io.github.fatalitaya.combatnexus.config.SettingsJson;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.config.DefinitionResources;
import java.util.*;
/** Prepared with the existing aggregate combat reload; never publishes independently. */
public final class BehaviorDefinitionResources {
    private BehaviorDefinitionResources() {}
    public static Map<NexusId, BehaviorDefinition> read(DefinitionResources manager) {
        String directoryName = "combatnexus_behaviors";
        var resources = manager.json(directoryName);
        if (resources.size() > 256) throw new IllegalArgumentException("Behavior definition capacity exceeded");
        Map<NexusId, BehaviorDefinition> definitions = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (var reader = entry.getValue().openAsReader()) {
                var id = DefinitionResources.definitionId(directoryName, entry.getKey());
                if (id.toString().length() > 256) throw new IllegalArgumentException("Behavior definition ID too long");
                var definition = BehaviorDefinition.decode(SettingsJson.read(reader));
                for (int index = 0; index < definition.plan().components().size(); index++) {
                    NexusId damageType = switch (definition.plan().components().get(index)) {
                        case BuiltinBehaviors.InstantDamage damage -> damage.damageType();
                        case BuiltinBehaviors.PeriodicDamage damage -> damage.damageType();
                        case EnvironmentDamage damage -> damage.damageType();
                        case ScheduledEnvironmentDamage damage -> damage.parameters().damageType();
                        default -> null;
                    };
                    if (damageType != null && manager.contains(NexusId.fromNamespaceAndPath(
                            damageType.getNamespace(), "damage_type/" + damageType.getPath() + ".json")) == false)
                        throw new IllegalArgumentException("Invalid behavior " + entry.getKey() + " components[" + index
                                + "].damage_type: unknown resource " + damageType);
                }
                if (definitions.putIfAbsent(id, definition) != null)
                    throw new IllegalArgumentException("Duplicate behavior " + id);
            } catch (java.io.IOException | RuntimeException error) { throw new IllegalArgumentException("Invalid behavior " + entry.getKey(), error); }
        });
        return Map.copyOf(definitions);
    }
}
