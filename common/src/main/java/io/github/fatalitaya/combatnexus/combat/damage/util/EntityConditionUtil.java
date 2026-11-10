package io.github.fatalitaya.combatnexus.combat.damage.util;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleTags;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts;

public final class EntityConditionUtil {

    private static final NexusId ENDER_DRAGON_ID =
            NexusId.fromNamespaceAndPath("minecraft", "ender_dragon");

    private static final NexusId WITHER_ID =
            NexusId.fromNamespaceAndPath("minecraft", "wither");

    private EntityConditionUtil() {
    }

    public static boolean isEntityType(
            EntityTypeFacts entity,
            NexusId expected
    ) {
        if (entity == null) {
            return false;
        }

        NexusId actual =
                entity.id();

        return expected.equals(actual);
    }

    public static boolean isEntityTypeTag(
            EntityTypeFacts entity,
            NexusId tag
    ) {
        return EntityTypeTagUtil.is(entity, tag);
    }

    public static boolean isBoss(EntityTypeFacts entity) {
        if (entity == null) {
            return false;
        }

        if (isEntityTypeTag(
                entity,
                DamageModuleTags.EntityTypes.BOSSES
        )) {
            return true;
        }

        NexusId id =
                entity.id();

        /*
         * Vanilla fallback.
         * Datapacks can add more boss-like entities through:
         * data/combatnexus/tags/entity_type/bosses.json
         */
        return ENDER_DRAGON_ID.equals(id)
                || WITHER_ID.equals(id);
    }
}
