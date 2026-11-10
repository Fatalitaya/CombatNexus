package io.github.fatalitaya.combatnexus.combat.ability.api;

import java.util.*;

public record AbilityRequest(UUID requestId, AbilityGrant grant, AbilityTarget target, AbilityParameters input) {
    public AbilityRequest {
        Objects.requireNonNull(requestId); Objects.requireNonNull(grant);
        Objects.requireNonNull(target); Objects.requireNonNull(input);
    }
}
