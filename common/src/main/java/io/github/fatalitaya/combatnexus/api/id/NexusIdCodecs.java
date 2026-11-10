package io.github.fatalitaya.combatnexus.api.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public final class NexusIdCodecs {
    private NexusIdCodecs() {}
    public static final Codec<NexusId> CODEC = Codec.STRING.comapFlatMap(NexusIdCodecs::read, NexusId::toString).stable();
    private static DataResult<NexusId> read(String value) {
        try { return DataResult.success(NexusId.parse(value)); }
        catch (IllegalArgumentException invalid) { return DataResult.error(invalid::getMessage); }
    }
}
