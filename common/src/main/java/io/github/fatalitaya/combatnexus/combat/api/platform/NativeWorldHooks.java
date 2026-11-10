package io.github.fatalitaya.combatnexus.combat.api.platform;

import io.github.fatalitaya.combatnexus.effect.api.*;

/** Version-specific callbacks supplied once when loading a world. */
public interface NativeWorldHooks {
    NativeHealingApplication healing();
    ParticipantStateAccess participantStates();
    DamageTypeCatalog damageTypes();
    AbsorptionWorldAccess absorption();
    HealthWorldAccess health();
    io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileDamageWorldAccess projectileDamage();
    EquipmentObservation equipment();
    io.github.fatalitaya.combatnexus.combat.api.platform.EntityMotionAccess motion();
    io.github.fatalitaya.combatnexus.attribute.api.platform.AttributeWorldAccess attributes();
    boolean mayExpire(EffectInstance effect);
    void effectCommitted(EffectService.Change change);
}
