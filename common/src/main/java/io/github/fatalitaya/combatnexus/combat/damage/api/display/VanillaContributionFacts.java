package io.github.fatalitaya.combatnexus.combat.damage.api.display;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.*;
public final class VanillaContributionFacts {
    private VanillaContributionFacts() {}
    public static VanillaContributionFact vanillaBase(
            NexusId id,
            DamagePhase phase,
            NexusId channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceLabel
    ) {
        return new VanillaContributionFact(VanillaContributionFact.Kind.BASE, id, DamageContributionSourceKind.VANILLA_BRIDGE, phase, channel, bucket, null, value, sourceLabel, DamageContributionStatus.APPLIED);
    }
    public static VanillaContributionFact vanillaBase(
            NexusId id,
            DamageContributionSourceKind sourceKind,
            DamagePhase phase,
            NexusId channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceLabel
    ) {
        return new VanillaContributionFact(VanillaContributionFact.Kind.BASE, id, sourceKind, phase, channel, bucket, null, value, sourceLabel, DamageContributionStatus.APPLIED);
    }
    public static VanillaContributionFact vanillaMultiplier(
            NexusId id,
            DamagePhase phase,
            NexusId channel,
            DamageApplicationBucket bucket,
            NexusId preMultiplierBucket,
            float value,
            String sourceLabel
    ) {
        return new VanillaContributionFact(VanillaContributionFact.Kind.MULTIPLIER, id, DamageContributionSourceKind.VANILLA_BRIDGE, phase, channel, bucket, preMultiplierBucket, value, sourceLabel, DamageContributionStatus.APPLIED);
    }
    public static VanillaContributionFact vanillaTemporaryResistance(
            NexusId id,
            DamagePhase phase,
            NexusId channel,
            float value,
            String sourceLabel
    ) {
        return new VanillaContributionFact(VanillaContributionFact.Kind.RESISTANCE, id, DamageContributionSourceKind.VANILLA_BRIDGE, phase, channel, null, null, value, sourceLabel, DamageContributionStatus.APPLIED);
    }
    public static VanillaContributionFact vanillaMitigation(
            NexusId id,
            DamagePhase phase,
            NexusId channel,
            float value,
            String sourceLabel
    ) {
        return new VanillaContributionFact(VanillaContributionFact.Kind.MITIGATION, id, DamageContributionSourceKind.VANILLA_BRIDGE, phase, channel, null, null, value, sourceLabel, DamageContributionStatus.APPLIED);
    }
    public static VanillaContributionFact vanillaArmorEffectiveness(
            NexusId id,
            DamagePhase phase,
            float value,
            String sourceLabel
    ) {
        return new VanillaContributionFact(VanillaContributionFact.Kind.ARMOR, id, DamageContributionSourceKind.VANILLA_BRIDGE, phase, null, null, null, value, sourceLabel, DamageContributionStatus.APPLIED);
    }
    public static VanillaContributionFact vanillaEnchantmentBase(
            NexusId id,
            DamagePhase phase,
            NexusId channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceLabel
    ) {
        return new VanillaContributionFact(VanillaContributionFact.Kind.BASE, id, DamageContributionSourceKind.VANILLA_ENCHANTMENT, phase, channel, bucket, null, value, sourceLabel, DamageContributionStatus.APPLIED);
    }
    public static VanillaContributionFact vanillaMobEffectBase(
            NexusId id,
            DamagePhase phase,
            NexusId channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceLabel
    ) {
        return new VanillaContributionFact(VanillaContributionFact.Kind.BASE, id, DamageContributionSourceKind.VANILLA_MOB_EFFECT, phase, channel, bucket, null, value, sourceLabel, DamageContributionStatus.APPLIED);
    }
}
