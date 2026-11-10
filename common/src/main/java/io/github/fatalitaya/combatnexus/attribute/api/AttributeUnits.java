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
    public static Optional<Unit> unit(NexusId attribute) {
        var nativeCatalog = io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get();
        for (var kind : io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Vanilla.values()) {
            if (nativeCatalog.vanilla(kind).equals(attribute)) return Optional.of(switch (kind) {
                case ATTACK_DAMAGE, MAX_HEALTH, MAX_ABSORPTION -> Unit.HEALTH_POINTS;
                case ARMOR, ARMOR_TOUGHNESS -> Unit.ARMOR_POINTS;
                case ATTACK_SPEED -> Unit.ATTACKS_PER_SECOND;
                case MOVEMENT_SPEED -> Unit.VANILLA_MOVEMENT;
            });
        }
        return Optional.ofNullable(UNITS.get(attribute));
    }

    private static Map<NexusId, Unit> definitions() {
        Map<NexusId, Unit> result = new LinkedHashMap<>();
        for (var definition : AttributeCatalog.definitions()) {
            result.put(definition.id(), definition.unit());
        }
        return Map.copyOf(result);
    }
}
