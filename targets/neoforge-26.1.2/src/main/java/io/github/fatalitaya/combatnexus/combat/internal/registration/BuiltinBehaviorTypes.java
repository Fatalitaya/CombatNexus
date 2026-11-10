package io.github.fatalitaya.combatnexus.combat.internal.registration;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Built-in gameplay registrations; adding another type does not change the storage or scheduler. */
public final class BuiltinBehaviorTypes {
    private static final com.mojang.serialization.MapCodec<TriggerLimit> LIMIT = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.xmap(TriggerLimit.Scope::valueOf, Enum::name).fieldOf("trigger_scope").forGetter(TriggerLimit::scope),
        Codec.intRange(1,64).fieldOf("maximum_triggers").forGetter(TriggerLimit::maximum)).apply(i, TriggerLimit::new));
    public static void register() {
        com.mojang.serialization.MapCodec<io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage> environment = RecordCodecBuilder.mapCodec(i -> i.group(
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.fieldOf("damage_type").forGetter(io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage::damageType),
            Codec.DOUBLE.fieldOf("amount").forGetter(io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage::amount),
            Codec.DOUBLE.fieldOf("minimum_health").forGetter(io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage::minimumHealth),
            Codec.INT.fieldOf("interval_ticks").forGetter(io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage::intervalTicks)).apply(i, io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage::new));
        add("environment_damage", io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage.class, environment);
        add("scheduled_environment_damage", io.github.fatalitaya.combatnexus.effect.api.ScheduledEnvironmentDamage.class, RecordCodecBuilder.mapCodec(i -> i.group(
            environment.forGetter(io.github.fatalitaya.combatnexus.effect.api.ScheduledEnvironmentDamage::parameters),
            Codec.intRange(0, 1200000).fieldOf("first_delay_ticks").forGetter(io.github.fatalitaya.combatnexus.effect.api.ScheduledEnvironmentDamage::initialDelayTicks)
        ).apply(i, io.github.fatalitaya.combatnexus.effect.api.ScheduledEnvironmentDamage::new)));
        BehaviorTypes.register(new BehaviorType<>(NexusId.fromNamespaceAndPath("combatnexus", "effect_capability"),
            io.github.fatalitaya.combatnexus.effect.api.EffectCapabilityBehavior.class,
            RecordCodecBuilder.mapCodec(i -> i.group(
                io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.fieldOf("capability").forGetter(io.github.fatalitaya.combatnexus.effect.api.EffectCapabilityBehavior::capability),
                Codec.INT.optionalFieldOf("first_delay_ticks", 1).forGetter(io.github.fatalitaya.combatnexus.effect.api.EffectCapabilityBehavior::initialDelayTicks))
                    .apply(i, io.github.fatalitaya.combatnexus.effect.api.EffectCapabilityBehavior::new))));
        add("lethal_protection", BuiltinBehaviors.LethalProtection.class, Codec.intRange(0, 1200000).optionalFieldOf("cooldown_ticks", 0).xmap(BuiltinBehaviors.LethalProtection::new, BuiltinBehaviors.LethalProtection::cooldownTicks));
        add("reflection", BuiltinBehaviors.Reflection.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("ratio").forGetter(BuiltinBehaviors.Reflection::ratio),
            LIMIT.codec().fieldOf("limit").forGetter(BuiltinBehaviors.Reflection::limit)).apply(i, BuiltinBehaviors.Reflection::new)));
        add("item_block_reflection", BuiltinBehaviors.ItemBlockReflection.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("ratio").forGetter(BuiltinBehaviors.ItemBlockReflection::ratio),
            LIMIT.codec().fieldOf("limit").forGetter(BuiltinBehaviors.ItemBlockReflection::limit)).apply(i, BuiltinBehaviors.ItemBlockReflection::new)));
        add("overflow_shield", BuiltinBehaviors.OverflowShield.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("ratio").forGetter(BuiltinBehaviors.OverflowShield::ratio),
            Codec.DOUBLE.fieldOf("capacity").forGetter(BuiltinBehaviors.OverflowShield::capacity),
            Codec.INT.fieldOf("duration_ticks").forGetter(BuiltinBehaviors.OverflowShield::durationTicks),
            LIMIT.codec().fieldOf("limit").forGetter(BuiltinBehaviors.OverflowShield::limit)).apply(i, BuiltinBehaviors.OverflowShield::new)));
        add("periodic_healing", BuiltinBehaviors.PeriodicHealing.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(BuiltinBehaviors.PeriodicHealing::amount),
            Codec.INT.fieldOf("interval_ticks").forGetter(BuiltinBehaviors.PeriodicHealing::intervalTicks),
            Codec.INT.optionalFieldOf("first_delay_ticks", -1).forGetter(BuiltinBehaviors.PeriodicHealing::firstDelayTicks),
            Codec.BOOL.optionalFieldOf("only_when_missing_health", false).forGetter(BuiltinBehaviors.PeriodicHealing::onlyWhenMissingHealth),
            Codec.STRING.xmap(PeriodicPhase::valueOf, Enum::name).optionalFieldOf("phase", PeriodicPhase.RESTART).forGetter(BuiltinBehaviors.PeriodicHealing::phase)).apply(i, BuiltinBehaviors.PeriodicHealing::new)));
        add("healing_additive", BuiltinBehaviors.HealingAdditive.class, Codec.DOUBLE.fieldOf("amount").xmap(BuiltinBehaviors.HealingAdditive::new, BuiltinBehaviors.HealingAdditive::amount));
        add("instant_healing", BuiltinBehaviors.InstantHealing.class, Codec.DOUBLE.fieldOf("amount").xmap(BuiltinBehaviors.InstantHealing::new, BuiltinBehaviors.InstantHealing::amount));
        add("instant_damage", BuiltinBehaviors.InstantDamage.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(BuiltinBehaviors.InstantDamage::amount),
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.fieldOf("damage_type").forGetter(BuiltinBehaviors.InstantDamage::damageType),
            io.github.fatalitaya.combatnexus.api.id.NexusUuidCodecs.CODEC.optionalFieldOf("direct").forGetter(BuiltinBehaviors.InstantDamage::direct)).apply(i, BuiltinBehaviors.InstantDamage::new)));

        add("periodic_damage", BuiltinBehaviors.PeriodicDamage.class, RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(BuiltinBehaviors.PeriodicDamage::amount),
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.fieldOf("damage_type").forGetter(BuiltinBehaviors.PeriodicDamage::damageType),
            Codec.INT.fieldOf("interval_ticks").forGetter(BuiltinBehaviors.PeriodicDamage::intervalTicks),
            Codec.FLOAT.optionalFieldOf("minimum_health", 0.0f).forGetter(BuiltinBehaviors.PeriodicDamage::minimumHealth),
            Codec.INT.optionalFieldOf("first_delay_ticks", -1).forGetter(BuiltinBehaviors.PeriodicDamage::firstDelayTicks),
            Codec.STRING.xmap(PeriodicPhase::valueOf, Enum::name).optionalFieldOf("phase", PeriodicPhase.RESTART).forGetter(BuiltinBehaviors.PeriodicDamage::phase)).apply(i, BuiltinBehaviors.PeriodicDamage::new)));
    }
    private static <T extends BehaviorComponent> void add(String id, Class<T> implementation, com.mojang.serialization.MapCodec<T> codec) {
        BehaviorTypes.register(new BehaviorType<>(NexusId.fromNamespaceAndPath("combatnexus", id), implementation, codec));
    }
}
