package io.github.fatalitaya.combatnexus.presentation.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeCalculation;
import java.util.List;
import java.util.Optional;

public record CarriedEffectObservation(NexusId effect, int amplifier, Optional<DisplayText> title,
        float probability, List<AttributeParameter> attributes) {
    public CarriedEffectObservation { java.util.Objects.requireNonNull(effect); java.util.Objects.requireNonNull(title); attributes = List.copyOf(attributes); }
    public record AttributeParameter(NexusId attribute, AttributeCalculation.Modifier modifier) {}
}
