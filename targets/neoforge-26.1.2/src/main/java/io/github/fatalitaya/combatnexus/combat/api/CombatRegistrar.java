package io.github.fatalitaya.combatnexus.combat.api;

import java.util.function.Consumer;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRegistrar;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldSettlement;

/** Startup-only registration capability; retaining it does not extend registration lifetime. */
public interface CombatRegistrar {
    void damageBody(NexusId id, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.LivingEntity> owner);
    void nativeDamageIngress(NexusId id, io.github.fatalitaya.combatnexus.combat.api.platform.NativeDamageIngress ingress);
    void abilityExecutor(NexusId id, io.github.fatalitaya.combatnexus.combat.ability.api.AbilityExecutor executor);
    io.github.fatalitaya.combatnexus.combat.driver.api.DriverRegistration externalDriver(io.github.fatalitaya.combatnexus.combat.driver.api.DriverDefinition definition);
    void itemBindings(NexusId contribution, NexusId item, io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec bindings);
    io.github.fatalitaya.combatnexus.combat.api.definition.CombatDefinitionRegistrar definitions();
    io.github.fatalitaya.combatnexus.combat.api.platform.EquipmentDamageAccess equipmentDamage();
    io.github.fatalitaya.combatnexus.combat.api.platform.NativeDamageAccess nativeDamage();
    io.github.fatalitaya.combatnexus.combat.api.platform.EffectOriginAccess effectOrigins();
    io.github.fatalitaya.combatnexus.combat.api.platform.WorldLifecycleAccess worldLifecycle();
    io.github.fatalitaya.combatnexus.combat.api.platform.NativeEffectsAccess nativeEffects();
    io.github.fatalitaya.combatnexus.combat.damage.api.display.phrase.RulePhraseRegistry ruleDescriptions();
    <T extends io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent> void effectDescriptor(NexusId behaviorId, Class<T> type, io.github.fatalitaya.combatnexus.effect.api.EffectBehaviorDescriptor<T> descriptor);
    void effectCapability(NexusId id, io.github.fatalitaya.combatnexus.effect.api.EffectCapability capability);
    void effectStacking(NexusId id, io.github.fatalitaya.combatnexus.effect.api.EffectStackingStrategy strategy);
    void environmentEffect(io.github.fatalitaya.combatnexus.effect.api.EnvironmentEffectDefinition definition);
    void effectDisplay(NexusId definition, io.github.fatalitaya.combatnexus.effect.api.EffectDisplay display);
    void effectCondition(NexusId id, io.github.fatalitaya.combatnexus.effect.api.EffectCondition condition);
    void settlementPolicy(io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy policy);
    <T extends io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent> void intent(io.github.fatalitaya.combatnexus.combat.api.scheduling.IntentType<T> type);
    <T> void runtimeState(io.github.fatalitaya.combatnexus.combat.state.api.StateKey<T> key);
    void observeDefinitions(NexusId id, Consumer<DefinitionView> observer);
    <T extends io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment> void observeMoment(NexusId id, Class<T> type, Consumer<T> observer);
    void sourceDescriptions(NexusId id, java.util.function.Function<io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame,java.util.List<io.github.fatalitaya.combatnexus.combat.api.source.SourceDescription>> descriptions);
    io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeRegistrar bridges();
    void damageFormula(NexusId id, io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy policy);
    <T> void facts(io.github.fatalitaya.combatnexus.combat.api.input.FactKey<T> key,
            java.util.function.Function<net.minecraft.world.entity.LivingEntity, T> collector);
    HealingRegistrar healing();
    void restoredSource(NexusId provider, Consumer<io.github.fatalitaya.combatnexus.combat.api.source.RestoredSource> owner);
    void sourceProvider(NexusId id, int priority, io.github.fatalitaya.combatnexus.combat.api.source.SourceProvider provider);
    <T extends io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent> void behavior(io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorType<T> type);
    void observeShield(NexusId id, int priority, Consumer<ShieldSettlement> observer);
    void observeState(NexusId id, int priority, Consumer<CommandResult<?>> observer);
}
