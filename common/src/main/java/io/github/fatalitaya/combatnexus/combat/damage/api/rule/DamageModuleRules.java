package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import io.github.fatalitaya.combatnexus.combat.damage.api.rule.builder.DamageRuleBuilder;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public final class DamageModuleRules {

    private DamageModuleRules() {
    }

    public static DamageRuleBuilder offensive(NexusId id) {
        return DamageRuleBuilder.offensive(id);
    }

    public static DamageRuleBuilder defensive(NexusId id) {
        return DamageRuleBuilder.defensive(id);
    }

    public static DamageRuleBuilder any(NexusId id) {
        return DamageRuleBuilder.any(id);
    }
}
