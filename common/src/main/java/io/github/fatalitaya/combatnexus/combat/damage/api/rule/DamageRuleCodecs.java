package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import com.mojang.serialization.Codec;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.entity.EntityCategory;

import java.util.Locale;

/**
 * Shared codecs for DamageModule rule JSON.
 * <p>
 * Important:
 * Damage channels must be stored as NexusId in serialized/data-driven rules.
 * Do not eagerly decode them into DamageChannel during JSON parsing, because
 * datapack channel definitions may be reloaded independently from rule data.
 */
public final class DamageRuleCodecs {
    /*
     * ---------------------------------------------------------------------
     * DamageModule ids
     * ---------------------------------------------------------------------
     */

    /**
     * Serialized damage channel reference.
     * <p>
     * This intentionally stays as NexusId.
     * Runtime code should resolve it through DamageChannelRegistry only when
     * applying the rule.
     */
    public static final Codec<NexusId> DAMAGE_CHANNEL_ID =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    /**
     * Serialized pre-multiplier bucket reference.
     * <p>
     * This also stays as NexusId because external API users may register
     * pre-multiplier buckets during setup before the registry is frozen.
     */
    public static final Codec<NexusId> PRE_MULTIPLIER_BUCKET_ID =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    /*
     * ---------------------------------------------------------------------
     * Minecraft registry ids
     * ---------------------------------------------------------------------
     */

    public static final Codec<NexusId> DAMAGE_TYPE_ID =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    public static final Codec<NexusId> ENTITY_TYPE_ID =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    public static final Codec<NexusId> MOB_EFFECT_ID =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    public static final Codec<NexusId> ATTRIBUTE_ID =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    public static final Codec<NexusId> ITEM_ID =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    /*
     * ---------------------------------------------------------------------
     * Tags
     * ---------------------------------------------------------------------
     */

    public static final Codec<NexusId> DAMAGE_TYPE_TAG =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    public static final Codec<NexusId> ENTITY_TYPE_TAG =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    public static final Codec<NexusId> ITEM_TAG =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    public static final Codec<NexusId> MOB_EFFECT_TAG =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC;

    /*
     * ---------------------------------------------------------------------
     * Enums
     * ---------------------------------------------------------------------
     */

    public static final Codec<EntityCategory> MOB_CATEGORY =
            Codec.STRING.xmap(
                    name -> EntityCategory.valueOf(name.toUpperCase(Locale.ROOT)),
                    category -> category.name().toLowerCase(Locale.ROOT)
            );

    public static final Codec<DamageApplicationBucket> DAMAGE_APPLICATION_BUCKET =
            DamageApplicationBucket.CODEC;

    /*
     * ---------------------------------------------------------------------
     * Numeric helpers
     * ---------------------------------------------------------------------
     */

    public static final Codec<Float> RATIO_0_TO_1 =
            Codec.floatRange(0.0f, 1.0f);

    public static final Codec<Float> NON_NEGATIVE_FLOAT =
            Codec.floatRange(0.0f, Float.MAX_VALUE);

    /**
     * Additive multiplier value.
     * <p>
     * Example:
     * 0.25 means +25%.
     * -0.20 means -20%.
     */
    public static final Codec<Float> ADDITIVE_MULTIPLIER =
            Codec.FLOAT;

    private DamageRuleCodecs() {
    }
}
