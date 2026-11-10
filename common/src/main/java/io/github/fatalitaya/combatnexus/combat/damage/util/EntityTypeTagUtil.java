package io.github.fatalitaya.combatnexus.combat.damage.util;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts;

public final class EntityTypeTagUtil {

    private EntityTypeTagUtil() {
    }

    public static boolean is(EntityTypeFacts type, NexusId tag) {
        return type != null
                && type.tags().contains(tag);
    }
}
