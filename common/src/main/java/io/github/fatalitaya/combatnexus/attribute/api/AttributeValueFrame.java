package io.github.fatalitaya.combatnexus.attribute.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Numeric input only; identity is shared with the provenance captured in the same pass. */
public record AttributeValueFrame(UUID entity, long gameTick, long revision, Map<NexusId, Value> values) {
    public AttributeValueFrame {
        Objects.requireNonNull(entity);
        if (revision < 0) throw new IllegalArgumentException("Negative revision");
        values = Map.copyOf(values);
    }
    public float valueOrZero(NexusId attribute) {
        var value = values.get(attribute);
        if (value == null) return 0;
        float result = (float)value.effective();
        return Float.isFinite(result) ? result : 0;
    }
    public record Value(double base, double raw, double effective, NexusId effectiveReason) {
        public Value {
            Objects.requireNonNull(effectiveReason);
            if (!Double.isFinite(base) || !Double.isFinite(raw) || !Double.isFinite(effective))
                throw new IllegalArgumentException("Non-finite attribute");
        }
    }
}
