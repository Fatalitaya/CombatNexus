package io.github.fatalitaya.combatnexus.combat.api.bridge;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record DamageFormulaRequest(UUID invocation, NexusId policy, UUID target, Optional<UUID> attacker,
        double amount, Optional<UUID> action, Optional<io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch> launch) {
    public DamageFormulaRequest {
        Objects.requireNonNull(invocation); Objects.requireNonNull(policy); Objects.requireNonNull(target);
        Objects.requireNonNull(attacker); Objects.requireNonNull(action);
        Objects.requireNonNull(launch);
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid formula input");
    }
}
