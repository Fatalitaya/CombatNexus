package io.github.fatalitaya.combatnexus.combat.blocking.api;

import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;

/** Item blocking facts, separate from absorption, health and custom shield pools.
 * Amounts are measured at the native block boundary, before subsequent reductions.
 * completed means the enclosing native hurt call returned normally, not that it dealt damage.
 */
public record ItemBlockReceipt(NexusId item, EquipmentPosition hand, float incoming,
        float blocked, float remaining, boolean completed) {
    public ItemBlockReceipt {
        Objects.requireNonNull(item);
        if (hand != EquipmentPosition.MAINHAND && hand != EquipmentPosition.OFFHAND)
            throw new IllegalArgumentException("Item blocking requires a hand");
        if (!Float.isFinite(incoming) || incoming <= 0 || !Float.isFinite(blocked) || blocked <= 0
                || blocked > incoming || !Float.isFinite(remaining) || remaining < 0)
            throw new IllegalArgumentException("Invalid item block amounts");
    }
}
