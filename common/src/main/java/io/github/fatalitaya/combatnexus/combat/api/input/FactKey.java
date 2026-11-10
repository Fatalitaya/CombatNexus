package io.github.fatalitaya.combatnexus.combat.api.input;

import java.util.Objects;
import com.mojang.serialization.Codec;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Typed capture schema. Values must encode without consulting a live world. */
public record FactKey<T>(NexusId id, Codec<T> codec) {
    public FactKey { Objects.requireNonNull(id); Objects.requireNonNull(codec); }
}
