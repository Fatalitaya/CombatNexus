package io.github.fatalitaya.combatnexus.combat.api.behavior;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** Named parameter codec used by definitions and stored state. */
public record BehaviorType<T extends BehaviorComponent>(NexusId id, Class<T> implementation, MapCodec<T> codec) {
    public BehaviorType {
        Objects.requireNonNull(id); Objects.requireNonNull(implementation); Objects.requireNonNull(codec);
    }
}
