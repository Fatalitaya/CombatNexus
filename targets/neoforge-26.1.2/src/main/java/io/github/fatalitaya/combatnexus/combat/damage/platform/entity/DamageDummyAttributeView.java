package io.github.fatalitaya.combatnexus.combat.damage.platform.entity;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Immutable, runtime-reference-free presentation data for one real attribute. */
public record DamageDummyAttributeView(
        NexusId id,
        String translationKey,
        double baseValue,
        double effectiveValue,
        double defaultValue
) {

}
