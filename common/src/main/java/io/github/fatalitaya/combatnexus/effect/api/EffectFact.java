package io.github.fatalitaya.combatnexus.effect.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record EffectFact(EffectInstance effect, NexusId condition, Map<String, Double> values, NexusId stacking) {
    public EffectFact {
        Objects.requireNonNull(effect); Objects.requireNonNull(condition); Objects.requireNonNull(stacking); values = Map.copyOf(values);
        if (values.values().stream().anyMatch(v -> !Double.isFinite(v))) throw new IllegalArgumentException("Non-finite environment fact");
    }
}
