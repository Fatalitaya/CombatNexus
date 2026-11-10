package io.github.fatalitaya.combatnexus.combat.ability.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit;
import java.util.Objects;

/** Automatic triggers use existing source counters and periodic scheduling. */
public record AbilityTrigger(Kind kind, TriggerLimit limit, int intervalTicks, int firstDelayTicks) {
    public enum Kind { ACTIVE, DAMAGE, HEALING, PERIODIC }
    public static final AbilityTrigger ACTIVE = new AbilityTrigger(Kind.ACTIVE, TriggerLimit.ONCE, 0, 0);
    private static final Codec<TriggerLimit> LIMIT = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.xmap(TriggerLimit.Scope::valueOf, Enum::name).fieldOf("scope").forGetter(TriggerLimit::scope),
        Codec.INT.fieldOf("maximum").forGetter(TriggerLimit::maximum)).apply(i, TriggerLimit::new));
    public static final Codec<AbilityTrigger> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.xmap(Kind::valueOf, Enum::name).fieldOf("kind").forGetter(AbilityTrigger::kind),
        LIMIT.optionalFieldOf("limit", TriggerLimit.ONCE).forGetter(AbilityTrigger::limit),
        Codec.INT.optionalFieldOf("interval_ticks", 0).forGetter(AbilityTrigger::intervalTicks),
        Codec.INT.optionalFieldOf("first_delay_ticks", 0).forGetter(AbilityTrigger::firstDelayTicks)
    ).apply(i, AbilityTrigger::new));
    public AbilityTrigger {
        Objects.requireNonNull(kind); Objects.requireNonNull(limit);
        if (kind == Kind.PERIODIC) {
            if (intervalTicks < 1 || intervalTicks > 1200000 || firstDelayTicks < 0 || firstDelayTicks > 1200000)
                throw new IllegalArgumentException("Invalid periodic ability timing");
        } else if (intervalTicks != 0 || firstDelayTicks != 0) throw new IllegalArgumentException("Nonperiodic ability has timing");
        if (kind == Kind.ACTIVE && !limit.equals(TriggerLimit.ONCE))
            throw new IllegalArgumentException("Active abilities do not declare a trigger counter");
        if (kind == Kind.PERIODIC && limit.scope() == TriggerLimit.Scope.ACTION)
            throw new IllegalArgumentException("Periodic ability has no upstream action");
    }
}
