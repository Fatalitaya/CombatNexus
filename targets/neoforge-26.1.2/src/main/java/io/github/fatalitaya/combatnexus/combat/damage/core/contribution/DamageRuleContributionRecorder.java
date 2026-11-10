package io.github.fatalitaya.combatnexus.combat.damage.core.contribution;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuntimeDamageRule;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamagePhaseAccess;

public final class DamageRuleContributionRecorder {

    private DamageRuleContributionRecorder() {
    }

    public static void recordOperation(
            DamagePhaseAccess ctx,
            DamagePhase runningPhase,
            RuntimeDamageRule runtimeRule,
            DamageRuleOperation operation,
            DamageMutationResult result,
            int operationIndex
    ) {
        if (ctx == null
                || runtimeRule == null
                || operation == null
                || result == null) {
            return;
        }

        var fact = DamageRuleContributionFacts.operation(
                ctx.definitions().generation(), runningPhase, runtimeRule, operation, result, operationIndex);
        ctx.contributions().recordFact(result, fact);
    }
}
