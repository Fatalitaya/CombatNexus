package io.github.fatalitaya.combatnexus.api.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.UUID;
import java.util.stream.IntStream;

/** UUIDs in the existing four-integer persisted format, independent of native utilities. */
public final class NexusUuidCodecs {
    private NexusUuidCodecs() {}

    public static final Codec<UUID> CODEC = Codec.INT_STREAM.comapFlatMap(stream -> {
        int[] words = stream.limit(5).toArray();
        if (words.length != 4) return DataResult.error(() -> "Expected exactly 4 UUID integers");
        return DataResult.success(new UUID(((long) words[0] << 32) | (words[1] & 0xffffffffL),
                ((long) words[2] << 32) | (words[3] & 0xffffffffL)));
    }, value -> IntStream.of((int) (value.getMostSignificantBits() >>> 32), (int) value.getMostSignificantBits(),
            (int) (value.getLeastSignificantBits() >>> 32), (int) value.getLeastSignificantBits()));
}
