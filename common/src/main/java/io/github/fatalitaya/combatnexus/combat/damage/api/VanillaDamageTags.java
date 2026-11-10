package io.github.fatalitaya.combatnexus.combat.damage.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Vanilla data identities consumed by the combat rules; matching belongs to the target. */
public final class VanillaDamageTags {
    private VanillaDamageTags() {}
    public static final NexusId BYPASSES_ARMOR = NexusId.withDefaultNamespace("bypasses_armor");
    public static final NexusId BYPASSES_COOLDOWN = NexusId.withDefaultNamespace("bypasses_cooldown");
    public static final NexusId BYPASSES_EFFECTS = NexusId.withDefaultNamespace("bypasses_effects");
    public static final NexusId BYPASSES_ENCHANTMENTS = NexusId.withDefaultNamespace("bypasses_enchantments");
    public static final NexusId BYPASSES_INVULNERABILITY = NexusId.withDefaultNamespace("bypasses_invulnerability");
    public static final NexusId BYPASSES_RESISTANCE = NexusId.withDefaultNamespace("bypasses_resistance");
    public static final NexusId IS_EXPLOSION = NexusId.withDefaultNamespace("is_explosion");
    public static final NexusId IS_FIRE = NexusId.withDefaultNamespace("is_fire");
    public static final NexusId IS_PROJECTILE = NexusId.withDefaultNamespace("is_projectile");
}
