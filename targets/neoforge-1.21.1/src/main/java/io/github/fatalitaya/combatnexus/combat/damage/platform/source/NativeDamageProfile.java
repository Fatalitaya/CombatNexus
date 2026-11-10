package io.github.fatalitaya.combatnexus.combat.damage.platform.source;

import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.effect.platform.NativeOffensiveEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public final class NativeDamageProfile {
    private NativeDamageProfile() {}
    public static VanillaDamageSourceProfile capture(DamageSource source, LivingEntity attacker) {
        var sourceEntity = source.getEntity();
        var direct = source.getDirectEntity();
        var living = attacker != null ? attacker : sourceEntity instanceof LivingEntity entity ? entity : null;
        return VanillaDamageSourceProfile.create(source.typeHolder().unwrapKey().map(key -> NativeIds.nexus(key.location())).orElse(null),
                source.type().msgId(), direct == null ? null : direct.getId(),
                living != null && sourceEntity == living && direct == living, direct instanceof Projectile,
                source.typeHolder().tags().map(tag -> NativeIds.nexus(tag.location())).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                DamageSourceClassification.isMagic(source), () -> living == null ? null : NativeOffensiveEffects.capture(living));
    }
}
