package probe;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;

public record BudgetOperation(float stackingValue, NumericLimit numericLimit) implements DamageRuleOperation {
    @Override public NexusId type() { return NexusId.parse("probe:budget"); }
    @Override public DamageMutationResult apply(DamageRuleContext context) { return DamageMutationResult.APPLIED; }
}
