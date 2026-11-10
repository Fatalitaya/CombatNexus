package io.github.fatalitaya.combatnexus.combat.damage.api.enums;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/**
 * Damage channel value. Built-in {@code *_ID} fields are stable persistent IDs;
 * {@link #index()} is assigned at runtime and must never be serialized.
 */
public record DamageChannel(NexusId id, int index) {

    public static final NexusId UNTYPED_ID =
            DamageModuleIds.id("untyped");

    public static final NexusId PHYSICAL_ID =
            DamageModuleIds.id("physical");

    public static final NexusId FIRE_ID =
            DamageModuleIds.id("fire");

    public static final NexusId COLD_ID =
            DamageModuleIds.id("cold");

    public static final NexusId LIGHTNING_ID =
            DamageModuleIds.id("lightning");

    public static final NexusId MAGIC_ID =
            DamageModuleIds.id("magic");

    public static final NexusId WITHER_ID =
            DamageModuleIds.id("wither");

    public static final NexusId KINETIC_ID =
            DamageModuleIds.id("kinetic");

    public static final NexusId POISON_ID =
            DamageModuleIds.id("poison");

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DamageChannel other)) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
