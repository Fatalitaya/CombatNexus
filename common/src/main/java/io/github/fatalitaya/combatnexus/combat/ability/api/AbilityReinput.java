package io.github.fatalitaya.combatnexus.combat.ability.api;
import java.util.*;
public record AbilityReinput(UUID requestId,UUID actor,UUID execution,AbilityTarget target,AbilityParameters input) {
    public AbilityReinput { Objects.requireNonNull(requestId);Objects.requireNonNull(actor);Objects.requireNonNull(execution);Objects.requireNonNull(target);Objects.requireNonNull(input); }
}
