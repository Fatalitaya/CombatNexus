package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.effect.api.EffectFacts;

final class MobEffectTagConditionSupport {
    private MobEffectTagConditionSupport() {}
    static boolean matches(Iterable<EffectFacts.Observed> effects, NexusId tag) {
        for (var effect : effects) if (effect.tags().contains(tag)) return true;
        return false;
    }
}
