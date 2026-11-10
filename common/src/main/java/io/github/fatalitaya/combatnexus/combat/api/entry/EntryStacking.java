package io.github.fatalitaya.combatnexus.combat.api.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum EntryStacking {
    STACK,
    UNIQUE_ENTRY,
    UNIQUE_GROUP,
    REPLACE;

    public static final Codec<EntryStacking> CODEC =
            Codec.STRING.comapFlatMap(
                    EntryStacking::decode,
                    EntryStacking::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<EntryStacking> decode(String name) {
        for (EntryStacking value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage entry stacking mode: " + name);
    }
}
