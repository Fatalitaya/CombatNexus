package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Optional;

public final class DamageModuleOperations {

    private DamageModuleOperations() {
    }

    public static DamageRuleOperation addBaseDamage(
            NexusId channel,
            float value
    ) {
        return new AddBaseDamageOperation(channel, value);
    }

    public static DamageRuleOperation addBaseDamage(
            NexusId channel,
            DamageApplicationBucket applicationBucket,
            float value
    ) {
        return new AddBaseDamageOperation(channel, applicationBucket, value);
    }

    public static DamageRuleOperation addChannelPreMultiplier(
            NexusId channel,
            float value
    ) {
        return new AddChannelPreMultiplierOperation(
                channel,
                Optional.empty(),
                value
        );
    }

    public static DamageRuleOperation addChannelPreMultiplier(
            NexusId channel,
            NexusId preMultiplierBucket,
            float value
    ) {
        return new AddChannelPreMultiplierOperation(
                channel,
                Optional.of(preMultiplierBucket),
                value
        );
    }

    public static DamageRuleOperation addChannelPostMultiplier(
            NexusId channel,
            float value
    ) {
        return new AddChannelPostMultiplierOperation(channel, value);
    }

    public static DamageRuleOperation addGlobalPreMultiplier(float value) {
        return new AddGlobalPreMultiplierOperation(
                Optional.empty(),
                value
        );
    }

    public static DamageRuleOperation addGlobalPreMultiplier(
            NexusId preMultiplierBucket,
            float value
    ) {
        return new AddGlobalPreMultiplierOperation(
                Optional.of(preMultiplierBucket),
                value
        );
    }

    public static DamageRuleOperation addGlobalPostMultiplier(float value) {
        return new AddGlobalPostMultiplierOperation(value);
    }

    public static DamageRuleOperation convertDamage(
            NexusId from,
            NexusId to,
            float ratio
    ) {
        return new ConvertDamageOperation(from, to, ratio);
    }

    public static DamageRuleOperation gainExtraDamage(
            NexusId basedOn,
            NexusId to,
            float ratio
    ) {
        return new GainExtraDamageOperation(basedOn, to, ratio);
    }

    public static DamageRuleOperation addTemporaryResistance(
            NexusId channel,
            float value
    ) {
        return new AddTemporaryResistanceOperation(channel, value);
    }

    public static DamageRuleOperation addChannelMitigation(
            NexusId channel,
            float value
    ) {
        return new AddChannelMitigationOperation(channel, value);
    }

    public static DamageRuleOperation addGlobalMitigation(float value) {
        return new AddGlobalMitigationOperation(value);
    }

    public static DamageRuleOperation multiplyArmorEffectiveness(float value) {
        return new MultiplyArmorEffectivenessOperation(value);
    }

    public static DamageRuleOperation addTrueDamage(
            NexusId channel,
            float value
    ) {
        return new AddTrueDamageOperation(channel, value);
    }

    public static DamageRuleOperation overrideFinalDamage(float value) {
        return new OverrideFinalDamageOperation(value);
    }

    public static DamageRuleOperation cancelDamage() {
        return new CancelDamageOperation();
    }

    public static DamageRuleOperation cancelDamage(String sourceId) {
        return new CancelDamageOperation(sourceId);
    }
}
