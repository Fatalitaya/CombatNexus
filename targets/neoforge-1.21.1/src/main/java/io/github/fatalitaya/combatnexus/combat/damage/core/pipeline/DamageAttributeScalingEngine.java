package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.core.DamageComponent;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes;
import io.github.fatalitaya.combatnexus.combat.damage.registry.PreMultiplierBuckets;

/** Private one-shot handoff after all TYPE_SCALING callbacks have completed. */
final class DamageAttributeScalingEngine {
    private static final String TRACE_PREFIX = "attribute:";

    private DamageAttributeScalingEngine() {
    }

    static void execute(DamageModuleContext context) {
        if (!context.claimAttributeScaling()) {
            throw new IllegalStateException(
                    "Damage attribute scaling already executed for this transaction");
        }

        var components=new java.util.ArrayList<DamageComponent>();
        for(int i=0;i<context.getActiveComponentCount();i++)components.add(context.getActiveComponent(i));
        for(var contribution:DamageAttributeScaling.calculate(components,context.attackCategory(),context::getLogicalAttackerAttrOrZero,context::getVictimAttrOrZero)) {
            if(contribution.channel()==null)context.tryAddGlobalPreMultiplier(contribution.bucket(),contribution.value(),contribution.trace());
            else context.tryAddChannelPreMultiplier(contribution.channel(),contribution.bucket(),contribution.value(),contribution.trace());
        }
    }

    static float finiteOrZero(float value) {
        return DamageDecisionCalculations.finiteOrZero(value);
    }

    private static String traceId(NexusId attribute) {
        return TRACE_PREFIX + attribute;
    }
}
