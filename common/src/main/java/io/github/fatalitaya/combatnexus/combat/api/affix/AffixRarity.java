package io.github.fatalitaya.combatnexus.combat.api.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum AffixRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    UNIQUE;

    public static final Codec<AffixRarity> CODEC =
            Codec.STRING.comapFlatMap(
                    AffixRarity::decode,
                    AffixRarity::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<AffixRarity> decode(String name) {
        for (AffixRarity value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage affix rarity: " + name);
    }
}
