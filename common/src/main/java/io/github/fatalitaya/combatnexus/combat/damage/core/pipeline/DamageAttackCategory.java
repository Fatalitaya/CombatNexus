package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleTags;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile;
import io.github.fatalitaya.combatnexus.combat.damage.api.VanillaDamageTags;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Set;

/** Immutable internal attack classification fixed before pipeline execution. */
enum DamageAttackCategory {
    NONE,
    MELEE,
    PROJECTILE;

    static DamageAttackCategory classify(
            Set<NexusId> tags,
            VanillaDamageSourceProfile profile,
            boolean authoritativeDirectProjectile
    ) {
        return fromSignals(
                tags.contains(VanillaDamageTags.IS_PROJECTILE),
                tags.contains(DamageModuleTags.DamageTypes.IS_PROJECTILE),
                tags.contains(DamageModuleTags.DamageTypes.IS_RANGED),
                profile.directProjectile(),
                authoritativeDirectProjectile,
                profile.projectile(),
                tags.contains(DamageModuleTags.DamageTypes.IS_MELEE),
                profile.shouldApplyMeleeOffensiveMobEffects()
        );
    }

    static DamageAttackCategory fromSignals(
            boolean vanillaProjectileTag,
            boolean nexusProjectileTag,
            boolean rangedTag,
            boolean directProjectile,
            boolean authoritativeDirectProjectile,
            boolean vanillaProfileProjectile,
            boolean meleeTag,
            boolean directPlayerOrMobAttack
    ) {
        boolean projectile = vanillaProjectileTag
                || nexusProjectileTag
                || rangedTag
                || directProjectile
                || authoritativeDirectProjectile
                || vanillaProfileProjectile;
        if (projectile) {
            return PROJECTILE;
        }
        return meleeTag || directPlayerOrMobAttack ? MELEE : NONE;
    }
}
