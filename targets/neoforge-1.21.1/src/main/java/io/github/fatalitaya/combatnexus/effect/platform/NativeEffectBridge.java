package io.github.fatalitaya.combatnexus.effect.platform;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorPlan;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleApi;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.combat.api.platform.NativeEffectProjection;
import io.github.fatalitaya.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Version-bound effect capabilities and projection adapter. All timers live in the common service. */
@EventBusSubscriber(modid = "combatnexus")
public final class NativeEffectBridge {
    // Explicit combat ownership; other vanilla effects retain their native lifecycle.
    private static final Set<String> COMBAT_EFFECTS = Set.of(
        "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage",
        "jump_boost", "regeneration", "resistance", "fire_resistance", "invisibility", "blindness",
        "weakness", "poison", "wither", "health_boost", "absorption", "glowing", "levitation",
        "slow_falling", "darkness", "wind_charged", "weaving", "oozing", "infested");
    private static final Set<NexusId> MANAGED = new LinkedHashSet<>();
    private static final Map<NexusId, java.util.function.IntFunction<BehaviorPlan>> PLANS = new HashMap<>();
    private static final Map<LivingEntity, List<EffectInstance>> DEATH_CALLBACKS = new WeakHashMap<>();
    private record Pending(MobEffectInstance effect, UUID source, boolean force, io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance provenance, EffectApplicationPolicy application) {}
    private static final Map<LivingEntity, List<Pending>> PENDING = new WeakHashMap<>();
    private NativeEffectBridge() {}
    public static void registerProjection(NexusId id) { MANAGED.add(id); }
    @SubscribeEvent public static void register(io.github.fatalitaya.combatnexus.combat.platform.api.CombatRegisterEvent event) {
        for (var holder : BuiltInRegistries.MOB_EFFECT.holders().toList()) {
            var id = NativeIds.nexus(holder.key().location());
            if (!id.getNamespace().equals("minecraft") || !COMBAT_EFFECTS.contains(id.getPath())) continue;
            MANAGED.add(id);
            event.registrar().effectDisplay(id, new EffectDisplay(id, false, true, true, null, true, true, false));
            event.registrar().effectCapability(id, new EffectCapability() {
                public boolean execute(Context context) { return NativeEffectBridge.execute(holder, context); }
                public List<io.github.fatalitaya.combatnexus.presentation.api.DisplayText> describe(int amplifier) {
                    return io.github.fatalitaya.combatnexus.presentation.platform.VanillaCombatEffectDescriptions.describe(id, amplifier);
                }
            });
        }
        PLANS.put(NexusId.parse("minecraft:poison"), amplifier -> new BehaviorPlan(0, List.of(
            new io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.PeriodicDamage(1, NexusId.parse("neoforge:poison"), Math.max(1, 25 >> amplifier), 1, -1,
                io.github.fatalitaya.combatnexus.combat.api.behavior.PeriodicPhase.REMAINING_DURATION))));
        PLANS.put(NexusId.parse("minecraft:wither"), amplifier -> new BehaviorPlan(0, List.of(
            new io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.PeriodicDamage(1, NexusId.parse("minecraft:wither"), Math.max(1, 40 >> amplifier), 0, -1,
                io.github.fatalitaya.combatnexus.combat.api.behavior.PeriodicPhase.REMAINING_DURATION))));
        PLANS.put(NexusId.parse("minecraft:regeneration"), amplifier -> new BehaviorPlan(0, List.of(
            new io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.PeriodicHealing(1, Math.max(1, 50 >> amplifier), -1, true,
                io.github.fatalitaya.combatnexus.combat.api.behavior.PeriodicPhase.REMAINING_DURATION))));
        PLANS.put(NexusId.parse("minecraft:absorption"), amplifier -> new BehaviorPlan(0, List.of(new EffectCapabilityBehavior(NexusId.parse("minecraft:absorption"), 1))));
    }
    public static boolean managed(Holder<MobEffect> effect) {
        var id = NativeIds.nexus(effect.unwrapKey().orElseThrow().location());
        return MANAGED.contains(id) || EquipmentCombatAdapter.effects().projected(id);
    }
    public static boolean ready(LivingEntity entity) {
        return entity.level() instanceof ServerLevel level && CombatWorldSessions.current((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).isPresent()
                && level.getEntity(entity.getUUID()) == entity;
    }
    public static Optional<EffectInstance> state(LivingEntity entity, Holder<MobEffect> effect) {
        if (!(entity.level() instanceof ServerLevel level)) return Optional.empty();
        var id = NativeIds.nexus(effect.unwrapKey().orElseThrow().location());
        return CombatWorldSessions.managedEffects((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level), entity.getUUID()).stream().filter(e -> id.equals(e.display().nativeId())).findFirst();
    }
    public static MobEffectInstance projection(EffectInstance effect, long now) {
        var d = effect.display(); var s = effect.snapshot();
        return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolder(NativeIds.nativeId(d.nativeId())).orElseThrow(),
            s.expiresAtTick().isEmpty() ? -1 : (int)Math.min(Integer.MAX_VALUE, Math.max(0, s.expiresAtTick().getAsLong() - now)),
            s.level(), d.ambient(), d.particles(), d.icon());
    }
    public static void synchronizeDescriptions(LivingEntity entity, Collection<NexusId> previous) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)
                || player instanceof net.neoforged.neoforge.common.util.FakePlayer
                || !player.connection.hasChannel(io.github.fatalitaya.combatnexus.presentation.platform.network.EffectTooltipPayload.TYPE)) return;
        var states = CombatWorldSessions.managedEffects((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (player.level()), player.getUUID());
        var remaining = new HashSet<>(previous);
        for (var state : states) if (state.display().nativeId() != null) {
            var id = state.display().nativeId(); remaining.remove(id);
            String encoded;
            try {
            var snapshot = state.snapshot();
            var lifetime = snapshot.expiresAtTick().isEmpty() ? EffectLifetime.permanent()
                : EffectLifetime.timed((int)Math.min(Integer.MAX_VALUE, Math.max(1, snapshot.expiresAtTick().getAsLong() - io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.now(player, snapshot.source()))));
            var definition = new EffectDefinition(snapshot.level(), lifetime,
                state.stacking(),
                state.policy(), state.attributes(), state.behavior(), snapshot.restrictions());
            var block = EffectTooltip.nativeInstance(id, definition);
            var summary = new ArrayList<>(block.summary());
            var details = new ArrayList<>(block.details());
            if (state.display().environment()) {
                summary.add(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.environment." + id.getPath()));
                summary.add(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.effect.condition_ends"));
            }
            for (var layer : java.util.stream.Stream.concat(java.util.stream.Stream.of(state), state.hidden().stream()).toList())
                details.add(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.effect.source", io.github.fatalitaya.combatnexus.presentation.api.SourceProvenanceText.describe(layer.display().provenance())));
            var projected = new io.github.fatalitaya.combatnexus.presentation.api.TooltipBlock(block.title(), summary, details);
            encoded = io.github.fatalitaya.combatnexus.presentation.api.DescriptionDocument.CODEC.encodeStart(
                net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, player.registryAccess()), io.github.fatalitaya.combatnexus.presentation.api.DescriptionDocument.leaf(projected)).getOrThrow().toString();
            if (encoded.length() > 24000) encoded = "";
            } catch (Exception failure) {
                encoded = "";
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new io.github.fatalitaya.combatnexus.presentation.platform.network.EffectTooltipPayload(
                player.getUUID(), CombatWorldSessions.current((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (player.level())).orElseThrow(),
                io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleApi.definitionGeneration(io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (player.level().getServer())))), NativeIds.nexus(player.level().dimension().location()), id, encoded));
        }
        for (var id : remaining) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new io.github.fatalitaya.combatnexus.presentation.platform.network.EffectTooltipPayload(player.getUUID(), CombatWorldSessions.current((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (player.level())).orElseThrow(),
                io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleApi.definitionGeneration(io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (player.level().getServer())))), NativeIds.nexus(player.level().dimension().location()), id, ""));
    }
    public static Set<NexusId> tags(Holder<MobEffect> effect) {
        return effect.tags().map(tag -> NativeIds.nexus(tag.location())).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public static Set<ControlCategory> controlCategories(NexusId effect){
        if(!effect.namespace().equals("minecraft"))return Set.of();
        return switch(effect.path()){
            case "slowness","levitation" -> Set.of(ControlCategory.MOVEMENT);
            case "mining_fatigue" -> Set.of(ControlCategory.ACTION);
            case "blindness","darkness" -> Set.of(ControlCategory.PERCEPTION);
            default -> Set.of();
        };
    }
    public static boolean addForced(LivingEntity entity,MobEffectInstance incoming,Entity source){
        if(!managed(incoming.getEffect()))return ForcedNativeEffect.apply(entity,incoming,source);
        return add(entity,incoming,source,source==null?null:source.getUUID(),false,
            EquipmentCombatAdapter.effects().origin(entity,source==null?null:source.getUUID()),EffectApplicationPolicy.FORCE);
    }
    public static boolean add(LivingEntity entity, MobEffectInstance incoming, Entity source, boolean force) {
        return add(entity, incoming, source, source == null ? null : source.getUUID(), force, EquipmentCombatAdapter.effects().origin(entity, source == null ? null : source.getUUID()),EffectApplicationPolicy.NORMAL);
    }
    private static boolean add(LivingEntity entity, MobEffectInstance incoming, Entity source, UUID sourceId, boolean force, io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance provenance,EffectApplicationPolicy application) {
        if (!ready(entity)) {
            PENDING.computeIfAbsent(entity, ignored -> new ArrayList<>()).add(new Pending(new MobEffectInstance(incoming), sourceId, force, provenance,application));
            return true;
        }
        if (application==EffectApplicationPolicy.NORMAL&&!net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(entity, incoming, source)) return false;
        if(application==EffectApplicationPolicy.NORMAL){
            var id=NativeIds.nexus(incoming.getEffect().unwrapKey().orElseThrow().location());
            var runtime=io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing((ServerLevel)entity.level());
            if(runtime.effects().immune(entity.getUUID(),id,tags(incoming.getEffect()),controlCategories(id)))return false;
        }
        var old = state(entity, incoming.getEffect());
        var level = (ServerLevel)entity.level();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added(
            entity, old.map(e -> projection(e, io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.now(entity, e.snapshot().source()))).orElse(null), incoming, source));
        if (incoming.getEffect().value().isInstantenous()) {
            return instant(incoming.getEffect(), entity, source, source, incoming.getAmplifier(), 1,application).outcome() == EffectResult.Outcome.APPLIED;
        }
        if (incoming.getDuration() == 0) return false;
        var id = NativeIds.nexus(incoming.getEffect().unwrapKey().orElseThrow().location());
        var origin = NexusId.fromNamespaceAndPath("combatnexus", sourceId == null ? "unknown_effect_source" : "entity/" + sourceId);
        var attrs = new ArrayList<EffectInstance.AttributeContribution>();
        incoming.getEffect().value().createModifiers(incoming.getAmplifier(), (attribute, modifier) -> {
            if (entity.getAttribute(attribute) != null) attrs.add(new EffectInstance.AttributeContribution(NativeIds.nexus(attribute.unwrapKey().orElseThrow().location()),
                new AttributeSnapshot.Contribution(NativeIds.nexus(modifier.id()), modifier.amount(), io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess.get().operation(modifier))));
        });
        var lifetime = incoming.isInfiniteDuration() ? EffectLifetime.permanent() : EffectLifetime.timed(incoming.getDuration());
        var display = new EffectDisplay(id, incoming.isAmbient(), incoming.isVisible(), incoming.showIcon(), sourceId, true, true, false, provenance);
        var plan = PLANS.getOrDefault(id, amplifier -> new BehaviorPlan(0, List.of(new EffectCapabilityBehavior(id, 0)))).apply(incoming.getAmplifier());
        if (incoming.isInfiniteDuration()) plan = new BehaviorPlan(plan.priority(), plan.components().stream().map(component -> {
            if (component instanceof io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.PeriodicHealing healing)
                return (io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent)new io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.PeriodicHealing(
                    healing.amount(), healing.intervalTicks(), Math.floorMod(-io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.phase(entity,origin), healing.intervalTicks()), healing.onlyWhenMissingHealth(), healing.phase());
            if (component instanceof io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.PeriodicDamage damage)
                return (io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent)new io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.PeriodicDamage(
                    damage.amount(), damage.damageType(), damage.intervalTicks(), damage.minimumHealth(), Math.floorMod(-io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.phase(entity,origin), damage.intervalTicks()), damage.phase());
            return component;
        }).toList());
        var instance = new EffectInstance(new EffectSnapshot(entity.getUUID(), id, origin, incoming.getAmplifier(), lifetime.deadline(io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.now(entity, origin)), Set.of(), lifetime.kind()),
            attrs, io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED, plan,
            new EffectPolicy(EffectPolicy.Category.valueOf(incoming.getEffect().value().getCategory().name()),true,tags(incoming.getEffect()),new EffectControl(controlCategories(id),Set.of(),Set.of(),Set.of()),application), List.of(), display);
        var result = CombatAccess.find((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).orElseThrow().effects().apply(new EffectApplication(UUID.randomUUID(), instance,
            force ? EffectService.Stacking.REPLACE : EffectService.Stacking.STRONGER_WITH_HIDDEN));
        if (result.outcome() != EffectResult.Outcome.APPLIED) return false;
        if (!force) {
            if (old.isEmpty()) incoming.getEffect().value().onEffectAdded(entity, incoming.getAmplifier());
            incoming.getEffect().value().onEffectStarted(entity, incoming.getAmplifier());
        }
        var after = state(entity, incoming.getEffect());
        return !old.equals(after);
    }
    public static boolean remove(LivingEntity entity, Holder<MobEffect> type, boolean event) {
        var current = state(entity, type);
        if (current.isEmpty() || !current.get().policy().dispellable()) return false;
        if (event && net.neoforged.neoforge.event.EventHooks.onEffectRemoved(entity, type, null)) return false;
        var s = current.orElseThrow().snapshot();
        return CombatAccess.find((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) ((ServerLevel)entity.level())).orElseThrow().effects()
            .remove(UUID.randomUUID(), entity.getUUID(), s.effect(), s.source(), EffectService.Removal.DISPELLED).applied();
    }
    public static void readyInputs(LivingEntity entity) {
        var pending = PENDING.remove(entity); if (pending == null) return;
        var level = (ServerLevel)entity.level();
        for (var value : pending) add(entity, value.effect(), value.source() == null ? null : level.getEntity(value.source()), value.source(), value.force(), value.provenance(),value.application());
    }
    public static EffectResult instant(Holder<MobEffect> type, LivingEntity target, Entity direct, Entity owner, int amplifier, double intensity) {
        return instant(type,target,direct,owner,amplifier,intensity,EffectApplicationPolicy.NORMAL);
    }
    private static EffectResult instant(Holder<MobEffect> type, LivingEntity target, Entity direct, Entity owner, int amplifier, double intensity,EffectApplicationPolicy application) {
        if (!Double.isFinite(intensity) || intensity < 0) throw new IllegalArgumentException("Invalid instant intensity");
        var id = NativeIds.nexus(type.unwrapKey().orElseThrow().location());
        io.github.fatalitaya.combatnexus.combat.api.behavior.InstantBehavior behavior;
        if (type.is(MobEffects.HARM) != target.isInvertedHealAndHarm())
            behavior = new io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.InstantDamage(Math.max(0, (int)(intensity * (6 << amplifier) + 0.5)),
                NexusId.withDefaultNamespace(direct == null ? "magic" : "indirect_magic"), Optional.ofNullable(direct).map(Entity::getUUID));
        else behavior = new io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors.InstantHealing(Math.max(0, (int)(intensity * (4 << amplifier) + 0.5)));
        var source = NexusId.fromNamespaceAndPath("combatnexus", owner == null ? "unknown_effect_source" : "entity/" + owner.getUUID());
        var instance = new EffectInstance(new EffectSnapshot(target.getUUID(), id, source, amplifier, OptionalLong.empty(), Set.of(), EffectLifetime.Kind.INSTANT),
            List.of(), io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED, new BehaviorPlan(0, List.of(behavior)), EffectPolicy.DEFAULT, List.of(),
            new EffectDisplay(id, false, false, false, owner == null ? null : owner.getUUID(), true, false, false, EquipmentCombatAdapter.effects().origin(target, owner == null ? null : owner.getUUID())));
        return CombatAccess.find((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) ((ServerLevel)target.level())).orElseThrow().effects().apply(new EffectApplication(UUID.randomUUID(), instance.withApplication(application), EffectService.Stacking.REPLACE));
    }
    public static boolean mayExpire(ServerLevel level, EffectInstance effect) {
                if (effect.display().nativeId() == null || !(level.getEntity(effect.snapshot().target()) instanceof LivingEntity entity)) return true;
                return !net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired(
                    entity, projection(effect, io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.now(entity, effect.snapshot().source())))).isCanceled();
    }
    public static void committed(ServerLevel level, EffectService.Change change) {
                Set<UUID> targets = new LinkedHashSet<>();
                change.before().forEach(e -> targets.add(e.snapshot().target())); change.after().forEach(e -> targets.add(e.snapshot().target()));
                for (var id : targets) if (level.getEntity(id) instanceof LivingEntity entity) {
                    if (change.removal().orElse(null) == EffectService.Removal.DEATH)
                        DEATH_CALLBACKS.computeIfAbsent(entity, ignored -> new ArrayList<>()).addAll(change.before());
                    ((NativeEffectProjection)entity).combatnexus$projectEffects(true);
                    synchronizeDescriptions(entity, change.before().stream().map(e -> e.display().nativeId()).filter(Objects::nonNull).toList());
                }
    }
    public static void trigger(LivingEntity entity, EffectInstance effect, EffectCapability.Trigger trigger,
            net.minecraft.world.damagesource.DamageSource damageSource, float damage) {
        EffectTriggers.dispatch(entity, effect, trigger, damageSource, damage);
    }
    public static void removed(LivingEntity entity, Entity.RemovalReason reason) {
        var captured = DEATH_CALLBACKS.remove(entity);
        if (reason != Entity.RemovalReason.KILLED) return;
        if (captured == null && entity.level() instanceof ServerLevel level) captured = CombatWorldSessions.managedEffects((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level), entity.getUUID());
        if (captured != null) for (var effect : captured) trigger(entity, effect, EffectCapability.Trigger.DEATH, null, 0);
    }
    private static boolean execute(Holder<MobEffect> type, EffectCapability.Context context) {
        var entity = context.entity(); var level = (ServerLevel)entity.level(); var state = context.effect().snapshot();
        int amplifier = state.level();
        switch (context.trigger()) {
            case HURT -> type.value().onMobHurt(entity, amplifier, context.damageSource(), context.damage());
            case DEATH -> type.value().onMobRemoved(entity, amplifier, Entity.RemovalReason.KILLED);
            case APPLY -> { } // Native application hooks also run for accepted weaker applications.
            case END -> { }
            case TICK -> {
                int phase = state.expiresAtTick().isEmpty() ? (int)io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.phase(entity,state.source())
                    : (int)Math.max(0, state.expiresAtTick().getAsLong() - io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.now(entity, state.source()));
                if (!type.value().shouldApplyEffectTickThisTick(phase, amplifier)) return true;
                return type.value().applyEffectTick(entity, amplifier);
            }
        }
        return true;
    }
}
