package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame;
import java.util.*;

/** One evaluation capture; executors do not receive a mutable world. */
public record AbilityInput(AbilityRequest request, AbilityDefinition definition,
                           CombatInputFrame actor, List<CombatInputFrame> targets) {
    public AbilityInput { Objects.requireNonNull(request); Objects.requireNonNull(definition); Objects.requireNonNull(actor); targets=List.copyOf(targets); }
}
