package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes;
import io.github.fatalitaya.combatnexus.combat.damage.core.DamageComponent;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.registry.PreMultiplierBuckets;

import java.util.*;
import java.util.function.Function;

@org.jetbrains.annotations.ApiStatus.Internal
public final class DamageAttributeScaling {
    private DamageAttributeScaling() {}

    public record Contribution(
            io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel channel,
            int bucket,
            float value,
            String trace) {}

    public static List<Contribution> calculate(
            List<DamageComponent> components,
            DamageAttackCategory category,
            Function<NexusId, Float> attacker,
            Function<NexusId, Float> victim) {
        var result = new ArrayList<Contribution>();
        float vulnerability =
                finiteOrZero(victim.apply(CombatAttributes.vulnerableDamageAdditive()));
        if (vulnerability != 0.0f) {
            result.add(
                    new Contribution(
                            null,
                            PreMultiplierBuckets.vulnerableDamage(),
                            vulnerability,
                            traceId(CombatAttributes.vulnerableDamageAdditive())));
        }
        int componentCount = components.size();
        for (int index = 0; index < componentCount; index++) {
            DamageComponent component = components.get(index);
            DamageAttributeMappings.ChannelMapping mapping =
                    DamageAttributeMappings.forChannel(component.channel);
            if (mapping == null) {
                continue;
            }
            float value = finiteOrZero(attacker.apply(mapping.attribute()));
            if (value == 0.0f) {
                continue;
            }
            int bucket = PreMultiplierBucketRegistry.getPreMultiplierBucketId(mapping.bucketId());
            result.add(
                    new Contribution(
                            component.channel, bucket, value, traceId(mapping.attribute())));
        }

        NexusId categoryAttribute =
                switch (category) {
                    case MELEE -> CombatAttributes.meleeDamageAdditive();
                    case PROJECTILE -> CombatAttributes.projectileDamageAdditive();
                    case NONE -> null;
                };
        if (categoryAttribute == null) {
            return List.copyOf(result);
        }
        float categoryValue = finiteOrZero(attacker.apply(categoryAttribute));
        if (categoryValue == 0.0f) {
            return List.copyOf(result);
        }
        result.add(
                new Contribution(
                        null,
                        PreMultiplierBuckets.genericDamage(),
                        categoryValue,
                        traceId(categoryAttribute)));
        return List.copyOf(result);
    }

    private static float finiteOrZero(float value) {
        return DamageDecisionCalculations.finiteOrZero(value);
    }

    private static String traceId(NexusId attribute) {
        return "attribute:" + attribute;
    }
}
