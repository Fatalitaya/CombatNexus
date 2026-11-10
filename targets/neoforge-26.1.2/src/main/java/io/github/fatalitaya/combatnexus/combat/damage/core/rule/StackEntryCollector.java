package io.github.fatalitaya.combatnexus.combat.damage.core.rule;

import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleExecutionContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuntimeDamageRule;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.RuntimeAffix;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.RuntimeEntry;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageFrozenFacts;
import io.github.fatalitaya.combatnexus.combat.damage.core.security.DamageModuleItemSecurity;
import io.github.fatalitaya.combatnexus.combat.damage.api.item.RuleItemView;

import java.util.List;

public final class StackEntryCollector {
    private StackEntryCollector() {
    }

    public static void collectStackEntries(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            RuleItemView stack,
            RuleExecutionContext exec,
            String source
    ) {
        collectStackEntries(
                ctx, phase, out, stack, exec, source, true, true
        );
    }

    public static void collectStackEntries(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            RuleItemView stack,
            RuleExecutionContext exec,
            String source,
            boolean readEntries,
            boolean readAffixes
    ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        DamageModuleItemSecurity.ValidatedItemRules validated =
                DamageModuleItemSecurity.validateForExecution(
                        stack,
                        source,
                        ctx.definitions()
                );

        if (validated.isEmpty()) {
            return;
        }

        if (readEntries) {
            collectEntryRules(
                    ctx,
                    phase,
                    out,
                    validated.entries(),
                    exec,
                    source
            );
        }

        if (readAffixes) {
            collectAffixRules(
                    ctx,
                    phase,
                    out,
                    validated.affixes(),
                    exec,
                    source
            );
        }
    }

    private static void collectEntryRules(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            List<EntryDefinition> entries,
            RuleExecutionContext exec,
            String source
    ) {
        if (entries.isEmpty()) {
            return;
        }

        for (EntryDefinition entry : entries) {
            RuntimeEntry runtimeEntry =
                    new RuntimeEntry(entry, exec);

            for (RuntimeDamageRule runtimeRule : runtimeEntry.expandRules()) {
                DamageRuleDefinition rule = runtimeRule.definition();

                if (rule.phase() != phase) {
                    continue;
                }

                if (!rule.role().canRunAs(exec.role())) {
                    continue;
                }

                out.add(runtimeRule);
            }
        }
    }

    private static void collectAffixRules(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            List<AffixDefinition> affixes,
            RuleExecutionContext exec,
            String source
    ) {
        if (affixes.isEmpty()) {
            return;
        }

        for (AffixDefinition affix : affixes) {
            RuntimeAffix runtimeAffix =
                    new RuntimeAffix(affix, exec);

            for (RuntimeDamageRule runtimeRule : runtimeAffix.expandRules()) {
                DamageRuleDefinition rule = runtimeRule.definition();

                if (rule.phase() != phase) {
                    continue;
                }

                if (!rule.role().canRunAs(exec.role())) {
                    continue;
                }

                out.add(runtimeRule);
            }
        }
    }

}
