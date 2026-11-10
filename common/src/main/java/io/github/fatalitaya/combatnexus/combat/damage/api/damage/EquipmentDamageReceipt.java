package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import java.util.Objects;

/** Equipment damage is neither health loss nor shield/absorption consumption. */
public record EquipmentDamageReceipt(EquipmentPosition slot, NexusId item, float absorbedDamage,
        int durabilityConsumed, boolean broken) {
    public EquipmentDamageReceipt {
        Objects.requireNonNull(slot); Objects.requireNonNull(item);
        if (!Float.isFinite(absorbedDamage) || absorbedDamage < 0 || durabilityConsumed < 0)
            throw new IllegalArgumentException("Invalid equipment damage receipt");
    }
}
