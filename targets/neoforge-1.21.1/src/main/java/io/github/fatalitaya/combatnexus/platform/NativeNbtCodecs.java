package io.github.fatalitaya.combatnexus.platform;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.*;
import java.util.Optional;
public final class NativeNbtCodecs {
    private NativeNbtCodecs() {}
    public static <T> void write(CompoundTag tag, String key, Codec<T> codec, T value) {
        tag.put(key, codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow());
    }
    public static <T> Optional<T> read(CompoundTag tag, String key, Codec<T> codec) {
        return tag.contains(key) ? Optional.of(codec.parse(NbtOps.INSTANCE, tag.get(key)).getOrThrow()) : Optional.empty();
    }
}
