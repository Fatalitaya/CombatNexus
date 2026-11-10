package io.github.fatalitaya.combatnexus.attribute.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Display semantics only; movement uses vanilla units and is never mislabeled as blocks/second. */
public final class AttributeUnits {
    public enum Unit { HEALTH_POINTS, ARMOR_POINTS, RATING, PROBABILITY, ADDITIVE_RATIO, CONVERSION_RATIO, MULTIPLIER,
        ATTACKS_PER_SECOND, VANILLA_MOVEMENT }
    private static final Map<NexusId, Unit> UNITS = definitions();
    private AttributeUnits() {}
    public static Optional<Unit> unit(NexusId attribute) { return Optional.ofNullable(UNITS.get(attribute)); }

    private static Map<NexusId, Unit> definitions() {
        Map<NexusId, Unit> result = new LinkedHashMap<>();
        for (var definition : AttributeCatalog.definitions()) {
            result.put(definition.id(), definition.unit());
        }
        result.put(NexusId.withDefaultNamespace("attack_damage"), Unit.HEALTH_POINTS);
        result.put(NexusId.withDefaultNamespace("max_health"), Unit.HEALTH_POINTS);
        result.put(NexusId.withDefaultNamespace("max_absorption"), Unit.HEALTH_POINTS);
        result.put(NexusId.withDefaultNamespace("armor"), Unit.ARMOR_POINTS);
        result.put(NexusId.withDefaultNamespace("armor_toughness"), Unit.ARMOR_POINTS);
        result.put(NexusId.withDefaultNamespace("attack_speed"), Unit.ATTACKS_PER_SECOND);
        result.put(NexusId.withDefaultNamespace("movement_speed"), Unit.VANILLA_MOVEMENT);
        return Map.copyOf(result);
    }
}
