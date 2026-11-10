package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.geometry.Position3;
import java.util.Objects;
import java.util.Optional;

/** Detached description; native source creation is deferred until authoritative application. */
public record DamageSourceDescriptor(NexusId damageType, Optional<Position3> sourcePosition) {
    public DamageSourceDescriptor {
        Objects.requireNonNull(damageType, "Damage type key must not be null");
        sourcePosition = sourcePosition == null ? Optional.empty() : sourcePosition;
    }
    public static DamageSourceDescriptor of(NexusId damageType) {
        return new DamageSourceDescriptor(damageType, Optional.empty());
    }
    public static DamageSourceDescriptor positioned(NexusId damageType, Position3 position) {
        return new DamageSourceDescriptor(damageType, Optional.of(Objects.requireNonNull(position, "Damage source position must not be null")));
    }
}
