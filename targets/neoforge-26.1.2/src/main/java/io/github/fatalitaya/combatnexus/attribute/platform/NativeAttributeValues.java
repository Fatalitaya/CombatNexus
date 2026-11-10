package io.github.fatalitaya.combatnexus.attribute.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeCalculation;
import io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess;
import java.util.Collection;
import java.util.Set;

/** Applies the registered native attribute's constraints after shared arithmetic. */
public final class NativeAttributeValues {
    private NativeAttributeValues() {}

    public static double valueWithout(NexusId attribute, double baseValue,
            Collection<AttributeCalculation.Modifier> modifiers, Set<NexusId> excludedModifierIds) {
        return AttributeAccess.get().sanitize(attribute,
                AttributeCalculation.valueWithout(baseValue, modifiers, excludedModifierIds));
    }
}
