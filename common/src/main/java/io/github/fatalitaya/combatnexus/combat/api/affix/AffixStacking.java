package io.github.fatalitaya.combatnexus.combat.api.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum AffixStacking {
    STACK,
    UNIQUE_AFFIX,
    UNIQUE_GROUP,
    HIGHEST_RARITY,
    REPLACE;

    public static final Codec<AffixStacking> CODEC =
            Codec.STRING.comapFlatMap(
                    AffixStacking::decode,
                    AffixStacking::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<AffixStacking> decode(String name) {
        for (AffixStacking value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage affix stacking mode: " + name);
    }
}
