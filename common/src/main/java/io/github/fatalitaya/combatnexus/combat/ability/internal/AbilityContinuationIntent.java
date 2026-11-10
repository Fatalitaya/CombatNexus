package io.github.fatalitaya.combatnexus.combat.ability.internal;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent;
import java.util.*;

/** A scheduled identity, not a permission or saved callback. */
public record AbilityContinuationIntent(UUID execution, int step) implements ScheduledIntent {
    public static final MapCodec<AbilityContinuationIntent> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.xmap(UUID::fromString,UUID::toString).fieldOf("execution").forGetter(AbilityContinuationIntent::execution),
        Codec.INT.fieldOf("step").forGetter(AbilityContinuationIntent::step)).apply(i,AbilityContinuationIntent::new));
    public AbilityContinuationIntent { Objects.requireNonNull(execution);if(step<1||step>63)throw new IllegalArgumentException("Invalid ability step"); }
}
