package io.github.fatalitaya.combatnexus.combat.api.affix;

import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOwner;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleExecutionContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuntimeDamageRule;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;

public record RuntimeAffix(
        AffixDefinition definition,
        RuleExecutionContext executionContext
) {
    public NexusId id() {
        return definition.id();
    }

    public List<RuntimeDamageRule> expandRules() {
        return definition.members()
                .stream()
                .flatMap(entry -> expandEntry(entry).stream())
                .toList();
    }

    private List<RuntimeDamageRule> expandEntry(
            AffixMember member
    ) {
        return member.entry().orElseThrow().contributions().stream().filter(io.github.fatalitaya.combatnexus.combat.api.entry.EntryContribution.Damage.class::isInstance).map(io.github.fatalitaya.combatnexus.combat.api.entry.EntryContribution.Damage.class::cast).filter(c->executionContext.matches(c.source()))
                .map(c -> new RuntimeDamageRule(
                        c.rule(),
                        executionContext.withContribution(c.id()),
                        DamageRuleOwner.affixEntry(
                                definition.id(),
                                member.id()
                        )
                ))
                .toList();
    }
}
