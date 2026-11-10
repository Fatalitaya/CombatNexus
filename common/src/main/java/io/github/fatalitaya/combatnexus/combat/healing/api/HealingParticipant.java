package io.github.fatalitaya.combatnexus.combat.healing.api;

import io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession;
import java.util.Objects;
import java.util.Optional;

/** Frozen healing input; the target adapter retains the matching native binding for this attempt. */
public record HealingParticipant(WorldSession world, ParticipantIdentity identity, boolean alive,
                                 Optional<CombatInputFrame> input) {
    public HealingParticipant {
        Objects.requireNonNull(world); Objects.requireNonNull(identity); Objects.requireNonNull(input);
        if (alive != input.isPresent()) throw new IllegalArgumentException("Living healing participant requires input");
        input.ifPresent(frame -> {
            if (!frame.entity().equals(identity.entity()) || !frame.sessionId().equals(world.id()))
                throw new IllegalArgumentException("Healing participant input identity mismatch");
        });
    }
}
