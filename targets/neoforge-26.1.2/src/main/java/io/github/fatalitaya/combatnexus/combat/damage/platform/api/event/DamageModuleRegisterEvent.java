package io.github.fatalitaya.combatnexus.combat.damage.platform.api.event;

import io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageModuleRegistrar;
import io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementListener;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageAttributionResolver;
import io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecisionProvider;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleProvider;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.source.EquippedItemRuleSource;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.neoforged.bus.api.Event;

import java.util.Objects;

/**
 * Fired on the NeoForge event bus during DamageModule common setup,
 * after built-in pre-multiplier buckets are registered and before
 * the pre-multiplier bucket registry is frozen.
 * <p>
 * Listen on the NeoForge/GAME bus, not the mod event bus.
 * Registration methods delegate to an expiring {@link DamageModuleRegistrar};
 * neither this event nor its registrar may be reused after event dispatch.
 */
public final class DamageModuleRegisterEvent extends Event {

    private final DamageModuleRegistrar registrar;

    public DamageModuleRegisterEvent(DamageModuleRegistrar registrar) {
        this.registrar = Objects.requireNonNull(registrar, "registrar");
    }

    public DamageModuleRegistrar registrar() {
        return registrar;
    }

    public void registerCondition(
            NexusId id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        registrar.registerCondition(id, codec);
    }

    public void registerOperation(
            NexusId id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        registrar.registerOperation(id, codec);
    }

    public int registerPreMultiplierBucket(NexusId id) {
        return registrar.registerPreMultiplierBucket(id);
    }

    public void registerRuleProvider(DamageRuleProvider provider) {
        registrar.registerRuleProvider(provider);
    }

    public void registerGlobalRule(DamageRuleDefinition rule) {
        registrar.registerGlobalRule(rule);
    }

    public io.github.fatalitaya.combatnexus.combat.api.pipeline.PipelineRegistration registerPhaseProcessor(NexusId id, DamagePhaseProcessor processor) {
        return registrar.registerPhaseProcessor(id, processor);
    }

    public void registerAttributionResolver(
            NexusId id,
            int priority,
            DamageAttributionResolver resolver
    ) {
        registrar.registerAttributionResolver(id, priority, resolver);
    }

    public void registerEquippedItemRuleSource(
            NexusId id,
            int priority,
            EquippedItemRuleSource source
    ) {
        registrar.registerEquippedItemRuleSource(id, priority, source);
    }

    public void registerCriticalDecisionProvider(
            NexusId id,
            int priority,
            CriticalDecisionProvider provider
    ) {
        registrar.registerCriticalDecisionProvider(id, priority, provider);
    }

    /**
     * Registers an authority-bearing server settlement callback with priority
     * in the inclusive range {@code -10000..10000}.
     */
    public void registerSettlementListener(
            NexusId id,
            int priority,
            DamageSettlementListener listener
    ) {
        registrar.registerSettlementListener(id, priority, listener);
    }

}
