package io.github.fatalitaya.combatnexus.combat.api.source;

import java.util.UUID;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;

/** Observed origin facts, independent of installation/stacking identity. Null means unknown. */
public record SourceProvenance(UUID entity, NexusId item, EquipmentPosition slot, NexusId enchantment, Block block) {
    public static final SourceProvenance UNKNOWN = new SourceProvenance(null, null, null, null, null);
    public static SourceProvenance entity(UUID entity) { return new SourceProvenance(entity, null, null, null, null); }
    public record Block(NexusId dimension, int x, int y, int z) {
        public Block { java.util.Objects.requireNonNull(dimension); }
    }
}
