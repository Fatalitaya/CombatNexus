package io.github.fatalitaya.combatnexus.effect.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess;
import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import io.github.fatalitaya.combatnexus.effect.api.EffectContributionIdentity;
import io.github.fatalitaya.combatnexus.effect.api.VanillaEffectContributions;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.HashSet;
import java.util.Set;

/** Reads native and managed modifier identities without installing contributions. */
public final class NativeOffensiveEffects {
    private NativeOffensiveEffects() {}
    public static VanillaEffectContributions.OffensiveObservation capture(LivingEntity entity) {
        var damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage == null) return new VanillaEffectContributions.OffensiveObservation(0, 0, 0, false, false);
        var strength = collectAttackDamageModifierIds(entity, MobEffects.STRENGTH);
        var weakness = collectAttackDamageModifierIds(entity, MobEffects.WEAKNESS);
        if (strength.isEmpty() && weakness.isEmpty())
            return new VanillaEffectContributions.OffensiveObservation(0, 0, 0, false, false);
        var modifiers = damage.getModifiers().stream().map(modifier -> new io.github.fatalitaya.combatnexus.attribute.api.AttributeCalculation.Modifier(
                AttributeAccess.get().modifierId(modifier), modifier.amount(), AttributeAccess.get().operation(modifier))).toList();
        var both = new java.util.HashSet<>(strength);
        both.addAll(weakness);
        var id = io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes.attackDamage();
        return new VanillaEffectContributions.OffensiveObservation(damage.getValue(),
            io.github.fatalitaya.combatnexus.attribute.platform.NativeAttributeValues.valueWithout(id, damage.getBaseValue(), modifiers, both),
            io.github.fatalitaya.combatnexus.attribute.platform.NativeAttributeValues.valueWithout(id, damage.getBaseValue(), modifiers, weakness),
            !strength.isEmpty(), !weakness.isEmpty());
    }
    private static Set<NexusId> collectAttackDamageModifierIds(
            LivingEntity attacker,
            Holder<MobEffect> effect
    ) {
        MobEffectInstance instance = attacker.getEffect(effect);

        if (instance == null) {
            return Set.of();
        }

        Set<NexusId> ids = new HashSet<>();
        if (attacker.level() instanceof net.minecraft.server.level.ServerLevel level) {
            var definition = ContentAccess.get().keyId(effect.unwrapKey().orElseThrow());
            for (var managed : io.github.fatalitaya.combatnexus.combat.api.CombatWorldSessions.managedEffects((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level), attacker.getUUID()))
                if (definition.equals(managed.display().nativeId()))
                    for (var contribution : managed.attributes())
                        if (contribution.attribute().equals(ContentAccess.get().keyId(Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow())))
                            ids.add(EffectContributionIdentity.id(managed, contribution));
        }

        effect.value().createModifiers(
                instance.getAmplifier(),
                (attribute, modifier) -> {
                    if (Attributes.ATTACK_DAMAGE.equals(attribute)) {
                        ids.add(AttributeAccess.get().modifierId(modifier));
                    }
                }
        );

        return ids;
    }

}
