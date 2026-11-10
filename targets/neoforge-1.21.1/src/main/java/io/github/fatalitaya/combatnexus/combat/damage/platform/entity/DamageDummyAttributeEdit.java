package io.github.fatalitaya.combatnexus.combat.damage.platform.entity;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** One requested base-value mutation in a server-validated batch. */
public record DamageDummyAttributeEdit(
        NexusId attributeId,
        double requestedBaseValue
) {

}
