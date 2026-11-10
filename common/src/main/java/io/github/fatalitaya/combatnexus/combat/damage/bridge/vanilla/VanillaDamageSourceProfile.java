package io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleTags;
import io.github.fatalitaya.combatnexus.combat.damage.api.VanillaDamageTags;
import io.github.fatalitaya.combatnexus.effect.api.VanillaEffectContributions.OffensiveObservation;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/** Captured damage facts; classification rules remain shared and never retain native entities. */
public record VanillaDamageSourceProfile(
        @Nullable NexusId damageTypeId, String msgId,
        @Nullable Integer directAttackerId, boolean directProjectile,
        Optional<OffensiveObservation> offensiveEffects,
        boolean directLivingAttack, boolean playerAttack, boolean mobAttack,
        boolean projectile, boolean explosion, boolean fire, boolean magic,
        boolean bypassesArmor, boolean bypassesEnchantments, boolean bypassesResistance, boolean bypassesCooldown
) {
    public VanillaDamageSourceProfile { java.util.Objects.requireNonNull(offensiveEffects); }
    public static VanillaDamageSourceProfile create(NexusId damageTypeId, String msgId, Integer directAttackerId,
            boolean directLivingAttack, boolean directProjectile, Set<NexusId> tags, boolean platformMagic,
            Supplier<OffensiveObservation> offensiveCapture) {
        boolean player = isId(damageTypeId, "player_attack");
        boolean mob = isId(damageTypeId, "mob_attack") || isId(damageTypeId, "mob_attack_no_aggro");
        boolean projectile = tags.contains(VanillaDamageTags.IS_PROJECTILE)
                || tags.contains(DamageModuleTags.DamageTypes.IS_PROJECTILE)
                || tags.contains(DamageModuleTags.DamageTypes.IS_RANGED) || directProjectile;
        var offensive = directLivingAttack && !projectile && (player || mob)
                ? Optional.ofNullable(offensiveCapture.get()) : Optional.<OffensiveObservation>empty();
        return new VanillaDamageSourceProfile(damageTypeId, msgId, directAttackerId, directProjectile, offensive,
                directLivingAttack, player, mob, projectile,
                tags.contains(VanillaDamageTags.IS_EXPLOSION) || tags.contains(DamageModuleTags.DamageTypes.IS_EXPLOSION),
                tags.contains(VanillaDamageTags.IS_FIRE) || tags.contains(DamageModuleTags.DamageTypes.IS_FIRE),
                platformMagic || tags.contains(DamageModuleTags.DamageTypes.IS_MAGIC),
                tags.contains(VanillaDamageTags.BYPASSES_ARMOR), tags.contains(VanillaDamageTags.BYPASSES_ENCHANTMENTS),
                tags.contains(VanillaDamageTags.BYPASSES_RESISTANCE), tags.contains(VanillaDamageTags.BYPASSES_COOLDOWN));
    }
    private static boolean isId(NexusId id, String path) {
        return id != null && id.getNamespace().equals("minecraft") && id.getPath().equals(path);
    }
    public boolean shouldApplyMeleeOffensiveMobEffects() { return directLivingAttack && !projectile && (playerAttack || mobAttack); }
}
