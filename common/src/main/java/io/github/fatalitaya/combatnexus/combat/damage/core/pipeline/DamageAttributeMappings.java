package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModulePreMultiplierBuckets;

import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Single authoritative mapping for the seven built-in channel attributes. */
final class DamageAttributeMappings {
    private static final List<ChannelMapping> CHANNEL_MAPPINGS = List.of(
            new ChannelMapping(DamageChannel.FIRE_ID,
                    DamageModuleIds.id("fire_damage_additive"),
                    () -> CombatAttributes.fireDamageAdditive(),
                    DamageModulePreMultiplierBuckets.FIRE_DAMAGE),
            new ChannelMapping(DamageChannel.COLD_ID,
                    DamageModuleIds.id("cold_damage_additive"),
                    () -> CombatAttributes.coldDamageAdditive(),
                    DamageModulePreMultiplierBuckets.COLD_DAMAGE),
            new ChannelMapping(DamageChannel.LIGHTNING_ID,
                    DamageModuleIds.id("lightning_damage_additive"),
                    () -> CombatAttributes.lightningDamageAdditive(),
                    DamageModulePreMultiplierBuckets.LIGHTNING_DAMAGE),
            new ChannelMapping(DamageChannel.MAGIC_ID,
                    DamageModuleIds.id("magic_damage_additive"),
                    () -> CombatAttributes.magicDamageAdditive(),
                    DamageModulePreMultiplierBuckets.MAGIC_DAMAGE),
            new ChannelMapping(DamageChannel.WITHER_ID,
                    DamageModuleIds.id("wither_damage_additive"),
                    () -> CombatAttributes.witherDamageAdditive(),
                    DamageModulePreMultiplierBuckets.WITHER_DAMAGE),
            new ChannelMapping(DamageChannel.POISON_ID,
                    DamageModuleIds.id("poison_damage_additive"),
                    () -> CombatAttributes.poisonDamageAdditive(),
                    DamageModulePreMultiplierBuckets.POISON_DAMAGE),
            new ChannelMapping(DamageChannel.KINETIC_ID,
                    DamageModuleIds.id("kinetic_damage_additive"),
                    () -> CombatAttributes.kineticDamageAdditive(),
                    DamageModulePreMultiplierBuckets.KINETIC_DAMAGE)
    );
    private static final Map<NexusId, ChannelMapping> BY_CHANNEL = index();

    private DamageAttributeMappings() {
    }

    static List<ChannelMapping> channelMappings() {
        return CHANNEL_MAPPINGS;
    }

    static ChannelMapping forChannel(DamageChannel channel) {
        return channel == null ? null : BY_CHANNEL.get(channel.id());
    }

    private static Map<NexusId, ChannelMapping> index() {
        HashMap<NexusId, ChannelMapping> mappings = new HashMap<>();
        Set<NexusId> attributes = new HashSet<>();
        Set<NexusId> buckets = new HashSet<>();
        for (ChannelMapping mapping : CHANNEL_MAPPINGS) {
            if (mappings.put(mapping.channelId(), mapping) != null
                    || !attributes.add(mapping.attributeId())
                    || !buckets.add(mapping.bucketId())) {
                throw new ExceptionInInitializerError(
                        "Duplicate built-in damage attribute mapping: " + mapping);
            }
        }
        return Map.copyOf(mappings);
    }

    record ChannelMapping(
            NexusId channelId,
            NexusId attributeId,
            Supplier<NexusId> attributeSupplier,
            NexusId bucketId
    ) {
        ChannelMapping {
            if (channelId == null || attributeId == null
                    || attributeSupplier == null || bucketId == null) {
                throw new NullPointerException("Damage attribute mapping fields");
            }
        }

        NexusId attribute() {
            return attributeSupplier.get();
        }
    }
}
