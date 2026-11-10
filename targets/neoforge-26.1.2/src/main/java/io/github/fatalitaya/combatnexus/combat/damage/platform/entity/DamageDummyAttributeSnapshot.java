package io.github.fatalitaya.combatnexus.combat.damage.platform.entity;

import java.util.List;
import java.util.Objects;
import io.github.fatalitaya.combatnexus.api.geometry.BlockPosition;

/** Authoritative server snapshot displayed by one pedestal menu. */
public record DamageDummyAttributeSnapshot(
        BlockPosition anchorPos,
        boolean available,
        List<DamageDummyAttributeView> attributes
) {



    public DamageDummyAttributeSnapshot {
        anchorPos = Objects.requireNonNull(anchorPos, "anchorPos");
        attributes = List.copyOf(Objects.requireNonNull(
                attributes,
                "attributes"
        ));
        if (attributes.size() > DamageDummyAttributeProtocol.MAX_ATTRIBUTES) {
            throw new IllegalArgumentException("too many damage-dummy attributes");
        }
        if (!available && !attributes.isEmpty()) {
            throw new IllegalArgumentException(
                    "unavailable snapshot cannot contain attributes"
            );
        }
    }

    public static DamageDummyAttributeSnapshot unavailable(BlockPosition anchorPos) {
        return new DamageDummyAttributeSnapshot(anchorPos, false, List.of());
    }
}
