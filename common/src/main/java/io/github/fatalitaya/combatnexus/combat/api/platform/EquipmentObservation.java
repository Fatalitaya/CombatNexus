package io.github.fatalitaya.combatnexus.combat.api.platform;

import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.combat.api.behavior.EquipmentItem;
import java.util.Optional;
import java.util.UUID;

/** World-owned equipment reads. Missing means the target is not loaded in this world. */
public interface EquipmentObservation {
    boolean alive(UUID target);
    Optional<EquipmentItem> item(UUID target, EquipmentPosition slot);
}
