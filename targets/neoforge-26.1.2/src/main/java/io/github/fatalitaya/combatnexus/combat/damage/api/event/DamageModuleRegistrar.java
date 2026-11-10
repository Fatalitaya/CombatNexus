package io.github.fatalitaya.combatnexus.combat.damage.api.event;

import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageAttributionResolver;
import io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecisionProvider;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.source.EquippedItemRuleSource;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleProvider;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/**
 * Registration capability supplied only while
 * the platform registration event is being dispatched.
 *
 * <p>Instances expire as soon as the event callback completes. Saving and
 * reusing one later is rejected even if the global lifecycle has not yet
 * finished freezing its internal registries.</p>
 */
public interface DamageModuleRegistrar {

    void registerCondition(
            NexusId id,
            MapCodec<? extends DamageRuleCondition> codec
    );

    void registerOperation(
            NexusId id,
            MapCodec<? extends DamageRuleOperation> codec
    );

    int registerPreMultiplierBucket(NexusId id);

    void registerRuleProvider(DamageRuleProvider provider);

    void registerGlobalRule(DamageRuleDefinition rule);

    io.github.fatalitaya.combatnexus.combat.api.pipeline.PipelineRegistration registerPhaseProcessor(NexusId id, DamagePhaseProcessor processor);

    /**
     * Registers a deterministic server-side attribution resolver. The ID is
     * retained verbatim and must use the registering mod's namespace.
     */
    void registerAttributionResolver(
            NexusId id,
            int priority,
            DamageAttributionResolver resolver
    );

    /**
     * Registers an external item-stack source without granting collector
     * access. The ID is retained verbatim and must use the registering mod's
     * namespace.
     */
    void registerEquippedItemRuleSource(
            NexusId id,
            int priority,
            EquippedItemRuleSource source
    );

    /**
     * Registers a deterministic server-side critical-decision provider.
     * Providers run once per transaction in descending priority order, then
     * by the full identifier. The callback-scoped collector is closed after
     * return and the registry freezes when this registration event ends.
     */
    void registerCriticalDecisionProvider(
            NexusId id,
            int priority,
            CriticalDecisionProvider provider
    );

    /**
     * Registers a server-side completed-damage callback. Callbacks are ordered
     * by descending priority, then by the full identifier, and receive child
     * authority only for their exact dynamic invocation. Priority must be in
     * the inclusive range {@code -10000..10000}.
     */
    void registerSettlementListener(
            NexusId id,
            int priority,
            DamageSettlementListener listener
    );

}
