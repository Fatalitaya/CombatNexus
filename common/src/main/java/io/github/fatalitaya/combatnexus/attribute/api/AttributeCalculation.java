package io.github.fatalitaya.combatnexus.attribute.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Collection;
import java.util.Set;

/** Shared modifier arithmetic. Native range constraints are applied by the platform after calculation. */
public final class AttributeCalculation {
    private AttributeCalculation() {}

    /** Raw values retain native non-finite inputs until the platform applies its range constraints. */
    public record Modifier(NexusId source, double amount, AttributeOperation operation) {
        public Modifier {
            java.util.Objects.requireNonNull(source);
            java.util.Objects.requireNonNull(operation);
        }
    }

    public static double valueWithout(double baseValue, Collection<Modifier> modifiers,
                                      Set<NexusId> excludedModifierIds) {
        double base = baseValue;
        for (var modifier : modifiers) {
            if (!excludedModifierIds.contains(modifier.source()) && modifier.operation() == AttributeOperation.ADD_VALUE) {
                base += modifier.amount();
            }
        }
        double result = base;
        for (var modifier : modifiers) {
            if (!excludedModifierIds.contains(modifier.source()) && modifier.operation() == AttributeOperation.ADD_MULTIPLIED_BASE) {
                result += base * modifier.amount();
            }
        }
        for (var modifier : modifiers) {
            if (!excludedModifierIds.contains(modifier.source()) && modifier.operation() == AttributeOperation.ADD_MULTIPLIED_TOTAL) {
                result *= 1.0D + modifier.amount();
            }
        }
        return result;
    }
}
