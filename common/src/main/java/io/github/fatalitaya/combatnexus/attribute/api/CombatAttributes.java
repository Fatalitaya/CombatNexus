package io.github.fatalitaya.combatnexus.attribute.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Stable, read-only references to DamageModule attributes. */
public final class CombatAttributes {
    public static final NexusId CRIT_CHANCE = key("crit_chance");
    public static final NexusId CRIT_DAMAGE_ADDITIVE = key("crit_damage_additive");
    /** Target-side additive incoming damage, assembled from the authoritative attribute snapshot. */
    public static final NexusId VULNERABLE_DAMAGE_ADDITIVE = key("vulnerable_damage_additive");
    public static final NexusId FIRE_DAMAGE_ADDITIVE = key("fire_damage_additive");
    public static final NexusId COLD_DAMAGE_ADDITIVE = key("cold_damage_additive");
    public static final NexusId LIGHTNING_DAMAGE_ADDITIVE = key("lightning_damage_additive");
    public static final NexusId MAGIC_DAMAGE_ADDITIVE = key("magic_damage_additive");
    public static final NexusId WITHER_DAMAGE_ADDITIVE = key("wither_damage_additive");
    public static final NexusId POISON_DAMAGE_ADDITIVE = key("poison_damage_additive");
    public static final NexusId KINETIC_DAMAGE_ADDITIVE = key("kinetic_damage_additive");
    public static final NexusId MELEE_DAMAGE_ADDITIVE = key("melee_damage_additive");
    public static final NexusId PROJECTILE_DAMAGE_ADDITIVE = key("projectile_damage_additive");
    /** Probability of avoiding an eligible primary attack and its descendants. */
    public static final NexusId DODGE_CHANCE = key("dodge_chance");
    public static final NexusId RESISTANCE_PHYSICAL = key("resistance_physical");
    public static final NexusId RESISTANCE_FIRE = key("resistance_fire");
    public static final NexusId RESISTANCE_COLD = key("resistance_cold");
    public static final NexusId RESISTANCE_LIGHTNING = key("resistance_lightning");
    public static final NexusId RESISTANCE_MAGIC = key("resistance_magic");
    public static final NexusId RESISTANCE_WITHER = key("resistance_wither");
    public static final NexusId RESISTANCE_POISON = key("resistance_poison");
    public static final NexusId RESISTANCE_KINETIC = key("resistance_kinetic");
    public static final NexusId RESISTANCE_MELEE = key("resistance_melee");
    public static final NexusId RESISTANCE_PROJECTILE = key("resistance_projectile");
    public static final NexusId ARMOR_PENETRATION_RATIO = key("armor_penetration_ratio");
    public static final NexusId ARMOR_PENETRATION_FLAT = key("armor_penetration_flat");
    public static final NexusId THORNS = key("thorns");
    /** Incoming healing multiplier, captured once by the independent healing pipeline. */
    public static final NexusId HEALING_POWER = key("healing_power");
    public static NexusId healingPower() { return HEALING_POWER; }
    public static final NexusId HEALING_RECEIVED = key("healing_received");

    private CombatAttributes() {}

    // Reuse vanilla identities; never register synonymous attributes.
    public static NexusId attackDamage() { return io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().vanilla(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Vanilla.ATTACK_DAMAGE); }
    public static NexusId attackSpeed() { return io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().vanilla(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Vanilla.ATTACK_SPEED); }
    public static NexusId movementSpeed() { return io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().vanilla(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Vanilla.MOVEMENT_SPEED); }
    public static NexusId armor() { return io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().vanilla(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Vanilla.ARMOR); }
    public static NexusId armorToughness() { return io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().vanilla(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Vanilla.ARMOR_TOUGHNESS); }
    public static NexusId maxHealth() { return io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().vanilla(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Vanilla.MAX_HEALTH); }

    /** Business identities are available independently of native registration. */
    public static NexusId critChance() { return CRIT_CHANCE; }
    public static NexusId critDamageAdditive() { return CRIT_DAMAGE_ADDITIVE; }
    public static NexusId vulnerableDamageAdditive() { return VULNERABLE_DAMAGE_ADDITIVE; }
    public static NexusId fireDamageAdditive() { return FIRE_DAMAGE_ADDITIVE; }
    public static NexusId coldDamageAdditive() { return COLD_DAMAGE_ADDITIVE; }
    public static NexusId lightningDamageAdditive() { return LIGHTNING_DAMAGE_ADDITIVE; }
    public static NexusId magicDamageAdditive() { return MAGIC_DAMAGE_ADDITIVE; }
    public static NexusId witherDamageAdditive() { return WITHER_DAMAGE_ADDITIVE; }
    public static NexusId poisonDamageAdditive() { return POISON_DAMAGE_ADDITIVE; }
    public static NexusId kineticDamageAdditive() { return KINETIC_DAMAGE_ADDITIVE; }
    public static NexusId meleeDamageAdditive() { return MELEE_DAMAGE_ADDITIVE; }
    public static NexusId projectileDamageAdditive() { return PROJECTILE_DAMAGE_ADDITIVE; }
    public static NexusId armorPenetrationRatio() { return ARMOR_PENETRATION_RATIO; }
    public static NexusId armorPenetrationFlat() { return ARMOR_PENETRATION_FLAT; }
    public static NexusId dodgeChance() { return DODGE_CHANCE; }
    public static NexusId resistancePhysical() { return RESISTANCE_PHYSICAL; }
    public static NexusId resistanceFire() { return RESISTANCE_FIRE; }
    public static NexusId resistanceCold() { return RESISTANCE_COLD; }
    public static NexusId resistanceLightning() { return RESISTANCE_LIGHTNING; }
    public static NexusId resistanceMagic() { return RESISTANCE_MAGIC; }
    public static NexusId resistanceWither() { return RESISTANCE_WITHER; }
    public static NexusId resistancePoison() { return RESISTANCE_POISON; }
    public static NexusId resistanceKinetic() { return RESISTANCE_KINETIC; }
    public static NexusId resistanceMelee() { return RESISTANCE_MELEE; }
    public static NexusId resistanceProjectile() { return RESISTANCE_PROJECTILE; }
    public static NexusId thorns() { return THORNS; }
    public static final NexusId LIFESTEAL = key("lifesteal");
    public static NexusId lifesteal() { return LIFESTEAL; }
    public static NexusId healingReceived() { return HEALING_RECEIVED; }


    private static NexusId key(String path) {
        NexusId id = NexusId.fromNamespaceAndPath("combatnexus", path);
        return id;
    }
}
