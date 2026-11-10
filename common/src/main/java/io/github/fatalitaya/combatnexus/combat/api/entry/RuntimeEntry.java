package io.github.fatalitaya.combatnexus.combat.api.entry;

import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOwner;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleExecutionContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuntimeDamageRule;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;

public record RuntimeEntry(
        EntryDefinition definition,
        RuleExecutionContext executionContext
) {
    public NexusId id() {
        return definition.id();
    }

    public List<RuntimeDamageRule> expandRules() {
        return definition.contributions().stream()
                .filter(EntryContribution.Damage.class::isInstance).map(EntryContribution.Damage.class::cast)
                .filter(c->executionContext.matches(c.source()))
                .map(c -> new RuntimeDamageRule(
                        c.rule(),
                        executionContext.withContribution(c.id()),
                        DamageRuleOwner.entry(definition.id())
                ))
                .toList();
    }
}
