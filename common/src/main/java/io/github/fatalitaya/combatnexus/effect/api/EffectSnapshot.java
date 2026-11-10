package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/** Effective source-owned restrictions; execution belongs to server action adapters. */
public record EffectSnapshot(UUID target, NexusId effect, NexusId source, int level,
                              OptionalLong expiresAtTick, Set<RestrictedAction> restrictions, EffectLifetime.Kind lifetime) {
    public EffectSnapshot(UUID target, NexusId effect, NexusId source, int level,
                          OptionalLong expiresAtTick, Set<RestrictedAction> restrictions) {
        this(target, effect, source, level, expiresAtTick, restrictions,
                expiresAtTick.isPresent() ? EffectLifetime.Kind.TIMED : EffectLifetime.Kind.PERMANENT);
    }
    public EffectSnapshot {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(expiresAtTick, "expiresAtTick");
        Objects.requireNonNull(lifetime, "lifetime");
        if ((lifetime == EffectLifetime.Kind.TIMED) != expiresAtTick.isPresent())
            throw new IllegalArgumentException("Only timed effects have a deadline");
        if (level < 0) throw new IllegalArgumentException("Negative effect level");
        restrictions = Set.copyOf(restrictions);
    }

    public enum RestrictedAction { WALK, JUMP, TURN, ATTACK, CAST, SKILL_MOVEMENT, EXTERNAL_MOVEMENT }
}
