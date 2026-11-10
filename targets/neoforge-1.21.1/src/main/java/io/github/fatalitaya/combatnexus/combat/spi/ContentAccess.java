package io.github.fatalitaya.combatnexus.combat.spi;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageType;
import java.util.Optional;
/** Stateless native operations, installed explicitly by the loader composition root. */
public final class ContentAccess {
    private ContentAccess() {}
    private static Port port;
    public static void install(Port implementation) {
        if (port != null) throw new IllegalStateException("ContentAccess already installed");
        port = java.util.Objects.requireNonNull(implementation);
    }
    public static Port get() { return java.util.Objects.requireNonNull(port, "ContentAccess not installed"); }
    public interface Port {
        io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageTypeFacts damageTypeFacts(net.minecraft.world.damagesource.DamageSource source);
        io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile damageProfile(net.minecraft.world.damagesource.DamageSource source, net.minecraft.world.entity.LivingEntity attacker);
        boolean technicalDamage(net.minecraft.world.damagesource.DamageSource source);
        io.github.fatalitaya.combatnexus.combat.damage.api.item.RuleItemView ruleItem(net.minecraft.world.item.ItemStack item);
        io.github.fatalitaya.combatnexus.combat.api.platform.HealthTarget healthTarget(net.minecraft.world.entity.LivingEntity entity);
        io.github.fatalitaya.combatnexus.attribute.api.platform.AttributeObservationAccess attributeObservation(net.minecraft.world.entity.LivingEntity entity);
        io.github.fatalitaya.combatnexus.effect.api.VanillaEffectContributions.OffensiveObservation offensiveEffects(net.minecraft.world.entity.LivingEntity entity);
        io.github.fatalitaya.combatnexus.effect.api.EffectFacts captureEffects(net.minecraft.world.entity.LivingEntity entity, java.util.function.IntConsumer budget);
        io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSourceDescriptor describeDamageSource(net.minecraft.world.damagesource.DamageSource source);
        net.minecraft.world.damagesource.DamageSource createDamageSource(io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageOrigin origin, Holder<DamageType> type);
        io.github.fatalitaya.combatnexus.api.entity.EntityCategory entityCategory(EntityType<?> type);
        boolean matchesDamageTag(net.minecraft.world.damagesource.DamageSource source, NexusId tag);
        io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts entityTypeFacts(EntityType<?> type);
        boolean matchesEntityTag(EntityType<?> type, NexusId tag);
        java.util.Set<NexusId> damageTags(Holder<DamageType> type);
        NexusId keyId(ResourceKey<?> key);
        NexusId itemId(Item item);
        NexusId entityTypeId(EntityType<?> type);
        NexusId effectId(MobEffect effect);
        Optional<Holder.Reference<MobEffect>> effect(NexusId id);
        ResourceKey<DamageType> damageKey(NexusId id);
    }
}
