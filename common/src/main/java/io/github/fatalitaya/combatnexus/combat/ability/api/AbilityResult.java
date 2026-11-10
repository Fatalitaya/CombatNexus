package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import java.util.*;

public record AbilityResult(Optional<AbilitySnapshot> execution, CommandResult<Boolean> receipt) {
    public AbilityResult { Objects.requireNonNull(execution); Objects.requireNonNull(receipt); }
}
