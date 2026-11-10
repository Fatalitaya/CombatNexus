package io.github.fatalitaya.combatnexus.combat.damage.core.request;

import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatLineage;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.HashSet;
import org.jetbrains.annotations.Nullable;

/** Damage trigger restrictions survive healing/shield hops in the common causal lineage. */
public final class DamageExecutionPolicy {
    private static final NexusId PROC = NexusId.fromNamespaceAndPath("combatnexus", "proc");
    private static final NexusId REFLECTION = NexusId.fromNamespaceAndPath("combatnexus", "reflection");
    private static final NexusId THORNS = NexusId.fromNamespaceAndPath("combatnexus", "thorns");
    private DamageExecutionPolicy() {}
    public static DamageTriggerPolicy inherited(CombatLineage lineage) {
        var blocked = lineage.suppressedTriggers();
        return new DamageTriggerPolicy(!blocked.contains(PROC), !blocked.contains(REFLECTION), !blocked.contains(THORNS));
    }
    public static DamageTriggerPolicy constrain(CombatLineage lineage, DamageTriggerPolicy policy) {
        var result = policy.intersect(inherited(lineage));
        var blocked = new HashSet<NexusId>();
        if (!result.procAllowed()) blocked.add(PROC);
        if (!result.reflectionAllowed()) blocked.add(REFLECTION);
        if (!result.thornsAllowed()) blocked.add(THORNS);
        lineage.suppressTriggers(blocked);
        return result;
    }
    public static DamageFailureReason failure(CombatLineage lineage, DamageRequestKind kind) {
        return triggerFailure(kind, inherited(lineage));
    }
    public static @Nullable DamageFailureReason triggerFailure(
            DamageRequestKind kind,
            @Nullable DamageTriggerPolicy parentPolicy
    ) {
        if (parentPolicy == null) {
            return null;
        }
        return switch (kind) {
            case PROC -> parentPolicy.procAllowed()
                    ? null
                    : DamageFailureReason.PROC_SUPPRESSED;
            case REFLECTED -> parentPolicy.reflectionAllowed()
                    ? null
                    : DamageFailureReason.REFLECTION_SUPPRESSED;
            case THORNS -> parentPolicy.thornsAllowed()
                    ? null
                    : DamageFailureReason.THORNS_SUPPRESSED;
            case PRIMARY, SWEEP, DOT, ENVIRONMENTAL, CUSTOM -> null;
        };
    }

    public static @Nullable DamageFailureReason depthFailure(
            int depth,
            int maximum
    ) {
        if (depth < 0 || maximum < 1) {
            throw new IllegalArgumentException(
                    "Depth must be non-negative and maximum must be positive"
            );
        }
        return depth > maximum
                ? DamageFailureReason.MAX_RECURSION_DEPTH
                : null;
    }

}
