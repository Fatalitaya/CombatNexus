package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum DamageRuleSourceFilter {
    /**
     * Any item-backed source, including equipment and captured projectile items.
     */
    ITEM,

    /**
     * An attacker/victim mainhand or offhand equipment source, or the captured
     * weapon used to create a projectile.
     */
    WEAPON,

    /**
     * An attacker/victim head, chest, legs, or feet equipment source.
     */
    ARMOR,

    /**
     * The captured item source of a projectile attack.
     */
    PROJECTILE;

    public static final Codec<DamageRuleSourceFilter> CODEC =
            Codec.STRING.comapFlatMap(
                    DamageRuleSourceFilter::decode,
                    DamageRuleSourceFilter::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<DamageRuleSourceFilter> decode(String name) {
        for (DamageRuleSourceFilter value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage entry slot: " + name);
    }
}
