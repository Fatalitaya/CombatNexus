package io.github.fatalitaya.combatnexus.combat.damage.core.rule;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleExecutionContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuntimeDamageRule;
import io.github.fatalitaya.combatnexus.combat.damage.core.contribution.DamageRuleContributionRecorder;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamagePhaseAccess;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageRuleContextViews;

public final class DamageRuleExecutor {
    private DamageRuleExecutor() {
    }

    public static void execute(
            DamagePhaseAccess ctx,
            DamagePhase runningPhase,
            RuntimeDamageRule runtimeRule
    ) {
        DamageRuleDefinition rule = runtimeRule.definition();

        if (rule.phase() != runningPhase) {
            return;
        }

        if (!rule.role().canRunAs(runtimeRule.executionContext().role())) {
            return;
        }

        for (DamageRuleCondition condition : rule.conditions()) {
            Boolean conditionResult =
                    safeTestCondition(
                            ctx,
                            runningPhase,
                            rule,
                            condition,
                            runtimeRule
                    );

            if (conditionResult == null) {
                /*
                 * Tolerant mode:
                 * A throwing condition invalidates this rule only.
                 */
                return;
            }

            if (!conditionResult) {
                return;
            }
        }

        if (rule.operations().isEmpty()) {
            return;
        }

        int operationIndex = 0;

        for (DamageRuleOperation operation : rule.operations()) {
            if (!isOperationAllowedInPhase(operation, runningPhase)) {
                DamageRuleContributionRecorder.recordOperation(
                        ctx,
                        runningPhase,
                        runtimeRule,
                        operation,
                        DamageMutationResult.REJECTED_WRONG_PHASE,
                        operationIndex
                );
                operationIndex++;
                continue;
            }

            DamageMutationResult result =
                    safeApplyOperation(
                            ctx,
                            runningPhase,
                            rule,
                            operation
                    );

            DamageRuleContributionRecorder.recordOperation(
                    ctx,
                    runningPhase,
                    runtimeRule,
                    operation,
                    result,
                    operationIndex
            );

            operationIndex++;

            if (ctx.isDamageCancelled()) {
                break;
            }
        }
    }

    public static boolean isOperationAllowedInPhase(
            DamageRuleOperation operation,
            DamagePhase phase
    ) {
        return operation.supportsPhase(phase);
    }

    private static Boolean safeTestCondition(
            DamagePhaseAccess ctx,
            DamagePhase runningPhase,
            DamageRuleDefinition rule,
            DamageRuleCondition condition,
            RuntimeDamageRule runtimeRule
    ) {
        io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageCheckpoint checkpoint =
                ctx.checkpointMutableState();
        try {
            return invokeCondition(
                    ctx,
                    condition,
                    runtimeRule.executionContext()
            );
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);

            return null;
        }
    }

    private static DamageMutationResult safeApplyOperation(
            DamagePhaseAccess ctx,
            DamagePhase runningPhase,
            DamageRuleDefinition rule,
            DamageRuleOperation operation
    ) {
        io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageCheckpoint checkpoint =
                ctx.checkpointMutableState();
        try {
            return invokeOperation(ctx, runningPhase, operation);
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);

            return DamageMutationResult.REJECTED_RULE_EXCEPTION;
        }
    }

    static boolean invokeCondition(
            NativeDamageRuleContext context,
            DamageRuleCondition condition,
            RuleExecutionContext executionContext
    ) {
        return condition.test(
                DamageRuleContextViews.readOnly(context),
                executionContext
        );
    }

    static DamageMutationResult invokeOperation(
            NativeDamageRuleContext context,
            DamagePhase phase,
            DamageRuleOperation operation
    ) {
        if (!isOperationAllowedInPhase(operation, phase)) {
            return DamageMutationResult.REJECTED_WRONG_PHASE;
        }

        return operation.apply(DamageRuleContextViews.restricted(context));
    }

}

