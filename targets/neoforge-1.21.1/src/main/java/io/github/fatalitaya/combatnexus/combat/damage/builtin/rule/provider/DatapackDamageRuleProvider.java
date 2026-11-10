package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.provider;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.*;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageInternalContexts;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageFrozenFacts;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.DatapackDamageRuleStore;

import java.util.List;

public final class DatapackDamageRuleProvider implements DamageRuleProvider {
    public static int ruleCount() {
        return DatapackDamageRuleStore.ruleCount();
    }

    @Override
    public void collect(
            NativeDamageRuleContext context,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        DamageFrozenFacts ctx = DamageInternalContexts.facts(
                context,
                "datapack rule provider"
        );

        for (DamageRuleDefinition rule
                : ctx.datapackRuleSnapshot().rules(phase)) {
            RuntimeDamageRule runtimeRule = new RuntimeDamageRule(
                    rule,
                    RuleExecutionContext.datapackRule(rule.role())
            );

            out.add(runtimeRule);
        }
    }
}

