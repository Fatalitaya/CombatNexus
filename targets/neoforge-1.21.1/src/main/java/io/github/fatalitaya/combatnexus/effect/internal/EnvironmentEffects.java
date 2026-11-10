package io.github.fatalitaya.combatnexus.effect.internal;

import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleApi;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.effect.api.*;

/** Common condition-binding and pulse coordinator; the registry owns all state-specific rules. */
public final class EnvironmentEffects {
    private static final Map<NexusId, EnvironmentEffectDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static boolean frozen;
    private static final ThreadLocal<Boolean> EXECUTING = ThreadLocal.withInitial(() -> false);
    private EnvironmentEffects() {}
    public static synchronized void register(EnvironmentEffectDefinition definition) {
        if (frozen) throw new IllegalStateException("Environment definitions frozen");
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) throw new IllegalArgumentException("Duplicate environment definition");
    }
    public static void freeze() { frozen = true; }
    public static Collection<EnvironmentEffectDefinition> definitions() { return List.copyOf(DEFINITIONS.values()); }
    public static void sample(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return;
        var api = CombatAccess.find((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)); if (api.isEmpty()) return;
        for (var definition : DEFINITIONS.values()) if (!definition.timed()) bind(api.get(), entity, definition, definition.condition().test(entity));
    }
    private static boolean bind(CombatWorld api, LivingEntity entity, EnvironmentEffectDefinition definition, boolean satisfied) {
        if (definition.timed()) return true;
        var id = definition.id();
        if (!satisfied && CombatWorldSessions.managedEffects((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) ((ServerLevel)entity.level()), entity.getUUID()).stream().noneMatch(e -> e.snapshot().effect().equals(id))) return true;
        var effect = new EffectInstance(new EffectSnapshot(entity.getUUID(), id, id, 0, OptionalLong.empty(), Set.of(), EffectLifetime.Kind.PERMANENT),
            List.of(), io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED, definition.behavior(entity), new EffectPolicy(EffectPolicy.Category.HARMFUL, false), List.of(),
            new EffectDisplay(id, false, false, true, null, false, false, true));
        var result = api.effects().bind(UUID.randomUUID(), new EffectBinding(effect, satisfied, EffectService.Stacking.REPLACE));
        return result.outcome() == EffectResult.Outcome.APPLIED || result.outcome() == EffectResult.Outcome.REMOVED;
    }
    public static void duration(LivingEntity entity, NexusId id, int remaining) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        var api = CombatAccess.find((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)); if (api.isEmpty()) return;
        var definition = DEFINITIONS.get(id);
        if (definition == null || !definition.timed()) throw new IllegalArgumentException("Expected timed environment definition");
        if (remaining <= 0) {
            if (CombatWorldSessions.managedEffects((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level), entity.getUUID()).stream().anyMatch(e -> e.snapshot().effect().equals(id)))
                api.get().effects().remove(UUID.randomUUID(), entity.getUUID(), id, id, EffectService.Removal.CONDITION_ENDED);
            return;
        }
        var effect = new EffectInstance(new EffectSnapshot(entity.getUUID(), id, id, 0, OptionalLong.of(Math.addExact(api.get().time().now(entity.getUUID(), id), remaining)), Set.of()),
            List.of(), io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED, definition.behavior(entity), new EffectPolicy(EffectPolicy.Category.HARMFUL, false), List.of(),
            new EffectDisplay(id, false, false, true, null, false, true, true));
        api.get().effects().apply(new EffectApplication(UUID.randomUUID(), effect, EffectService.Stacking.REPLACE));
    }
    public static int remaining(LivingEntity entity, NexusId id) {
        if (!(entity.level() instanceof ServerLevel level)) return 0;
        return CombatWorldSessions.managedEffects((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level), entity.getUUID()).stream().filter(e -> e.snapshot().effect().equals(id))
            .findFirst().map(e -> (int)Math.min(Integer.MAX_VALUE, Math.max(0, e.snapshot().expiresAtTick().orElse(io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.now(entity, e.snapshot().source())) - io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.now(entity, e.snapshot().source())))).orElse(0);
    }
    public static Optional<Boolean> pulse(LivingEntity entity, DamageSource source, float amount) {
        if (EXECUTING.get() || source.typeHolder().unwrapKey().isEmpty() || !(entity.level() instanceof ServerLevel level)) return Optional.empty();
        var type = ContentAccess.get().keyId(source.typeHolder().unwrapKey().orElseThrow());
        var definition = DEFINITIONS.values().stream().filter(d -> d.damageType().equals(type) && d.condition().test(entity)).findFirst();
        if (definition.isEmpty()) return Optional.empty();
        var api = CombatAccess.find((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)); if (api.isEmpty()) return Optional.empty();
        if (!bind(api.get(), entity, definition.get(), true)) return Optional.of(false);
        if (definition.get().scheduled()) return Optional.of(false);
        EXECUTING.set(true);
        try {
            var result = DamageModuleApi.submitDamage(DamageRequest.builder(level, entity, io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().describeDamageSource(source), amount)
                .minimumHealth((float)definition.get().minimumHealth().applyAsDouble(entity)).build());
            return Optional.of(result.status() == DamageSubmissionStatus.APPLIED);
        } finally { EXECUTING.remove(); }
    }
    public static void scheduled(LivingEntity entity, NexusId binding, EnvironmentDamage parameters) {
        var definition = DEFINITIONS.get(binding);
        if (definition == null || !definition.scheduled()) throw new IllegalArgumentException("Not a scheduled environment binding");
        var level = (ServerLevel)entity.level();
        if (!definition.condition().test(entity)) {
            bind(CombatAccess.find((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)).orElseThrow(), entity, definition, false);
            return;
        }
        if (parameters.amount() <= 0) return;
        var type = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
            .getOrThrow(ContentAccess.get().damageKey(parameters.damageType()));
        boolean previous = EXECUTING.get(); EXECUTING.set(true);
        try {
            DamageModuleApi.submitDamage(DamageRequest.builder(level, entity, io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().describeDamageSource(new DamageSource(type)), (float)parameters.amount())
                .minimumHealth((float)parameters.minimumHealth()).cooldownPolicy(DamageCooldownPolicy.INDEPENDENT).build());
        } finally { EXECUTING.set(previous); }
    }
}
