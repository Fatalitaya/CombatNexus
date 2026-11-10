package io.github.fatalitaya.combatnexus.platform;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageType;
import java.util.Optional;
import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import net.minecraft.core.registries.*;
public final class NativeContentAccess implements ContentAccess.Port {
    public io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageTypeFacts damageTypeFacts(net.minecraft.world.damagesource.DamageSource source) {
        return source == null ? null : new io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageTypeFacts(
                source.typeHolder().unwrapKey().map(key -> NativeIds.nexus(key.location())), damageTags(source.typeHolder()));
    }
    public io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile damageProfile(net.minecraft.world.damagesource.DamageSource source, net.minecraft.world.entity.LivingEntity attacker) {
        return io.github.fatalitaya.combatnexus.combat.damage.platform.source.NativeDamageProfile.capture(source, attacker);
    }
    public boolean technicalDamage(net.minecraft.world.damagesource.DamageSource source) { return io.github.fatalitaya.combatnexus.combat.damage.platform.source.DamageSourceClassification.isTechnical(source); }
    public io.github.fatalitaya.combatnexus.combat.damage.api.item.RuleItemView ruleItem(net.minecraft.world.item.ItemStack item) {
        return item == null ? null : new io.github.fatalitaya.combatnexus.combat.damage.platform.item.NativeRuleItem(item);
    }
    public io.github.fatalitaya.combatnexus.combat.api.platform.HealthTarget healthTarget(net.minecraft.world.entity.LivingEntity entity) { return new io.github.fatalitaya.combatnexus.combat.health.platform.NativeHealthBinding(entity); }
    public io.github.fatalitaya.combatnexus.attribute.api.platform.AttributeObservationAccess attributeObservation(net.minecraft.world.entity.LivingEntity entity) {
        return (requested, budget, previous) -> {
            var holders = new java.util.ArrayList<Holder<Attribute>>();
            for (var id : requested) holders.add(NativeAttributeAccess.resolve(id));
            return io.github.fatalitaya.combatnexus.attribute.platform.NativeAttributeCapture.observe(entity, holders, budget, previous);
        };
    }
    public io.github.fatalitaya.combatnexus.effect.api.VanillaEffectContributions.OffensiveObservation offensiveEffects(net.minecraft.world.entity.LivingEntity entity) { return io.github.fatalitaya.combatnexus.effect.platform.NativeOffensiveEffects.capture(entity); }
    public io.github.fatalitaya.combatnexus.effect.api.EffectFacts captureEffects(net.minecraft.world.entity.LivingEntity entity, java.util.function.IntConsumer budget) { return io.github.fatalitaya.combatnexus.effect.platform.NativeEffectFacts.capture(entity, budget); }
    public io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSourceDescriptor describeDamageSource(net.minecraft.world.damagesource.DamageSource source) { return io.github.fatalitaya.combatnexus.combat.damage.platform.source.NativeDamageSources.from(source); }
    public net.minecraft.world.damagesource.DamageSource createDamageSource(io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageOrigin origin, Holder<DamageType> type) { return io.github.fatalitaya.combatnexus.combat.damage.platform.source.NativeDamageSources.create(origin, type); }
    public io.github.fatalitaya.combatnexus.api.entity.EntityCategory entityCategory(EntityType<?> type) {
        return switch (type.getCategory()) {
            case MONSTER -> io.github.fatalitaya.combatnexus.api.entity.EntityCategory.MONSTER;
            case CREATURE -> io.github.fatalitaya.combatnexus.api.entity.EntityCategory.CREATURE;
            case AMBIENT -> io.github.fatalitaya.combatnexus.api.entity.EntityCategory.AMBIENT;
            case AXOLOTLS -> io.github.fatalitaya.combatnexus.api.entity.EntityCategory.AXOLOTLS;
            case UNDERGROUND_WATER_CREATURE -> io.github.fatalitaya.combatnexus.api.entity.EntityCategory.UNDERGROUND_WATER_CREATURE;
            case WATER_CREATURE -> io.github.fatalitaya.combatnexus.api.entity.EntityCategory.WATER_CREATURE;
            case WATER_AMBIENT -> io.github.fatalitaya.combatnexus.api.entity.EntityCategory.WATER_AMBIENT;
            case MISC -> io.github.fatalitaya.combatnexus.api.entity.EntityCategory.MISC;
        };
    }
    public boolean matchesDamageTag(net.minecraft.world.damagesource.DamageSource source, NexusId tag) { return source.is(damageTag(tag)); }
    public io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts entityTypeFacts(EntityType<?> type) {
        return new io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts(entityTypeId(type),
                BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).tags().map(tag -> NativeIds.nexus(tag.location())).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
    public boolean matchesEntityTag(EntityType<?> type, NexusId tag) { return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(entityTag(tag)); }
    public java.util.Set<NexusId> damageTags(Holder<DamageType> type) { return type.tags().map(tag -> NativeIds.nexus(tag.location())).collect(java.util.stream.Collectors.toUnmodifiableSet()); }

    public NexusId keyId(ResourceKey<?> key) { return NativeIds.nexus(key.location()); }
    public NexusId itemId(Item item) { return NativeIds.nexus(BuiltInRegistries.ITEM.getKey(item)); }
    public NexusId entityTypeId(EntityType<?> type) { return NativeIds.nexus(BuiltInRegistries.ENTITY_TYPE.getKey(type)); }
    public NexusId effectId(MobEffect effect) { return NativeIds.nexus(BuiltInRegistries.MOB_EFFECT.getKey(effect)); }
    public Optional<Holder.Reference<MobEffect>> effect(NexusId id) { return BuiltInRegistries.MOB_EFFECT.getHolder(NativeIds.nativeId(id)); }
    public ResourceKey<DamageType> damageKey(NexusId id) { return ResourceKey.create(Registries.DAMAGE_TYPE, NativeIds.nativeId(id)); }
    private TagKey<EntityType<?>> entityTag(NexusId id) { return TagKey.create(Registries.ENTITY_TYPE, NativeIds.nativeId(id)); }
    private TagKey<DamageType> damageTag(NexusId id) { return TagKey.create(Registries.DAMAGE_TYPE, NativeIds.nativeId(id)); }
}
