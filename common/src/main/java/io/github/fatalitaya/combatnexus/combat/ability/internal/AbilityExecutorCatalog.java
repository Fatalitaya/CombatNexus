package io.github.fatalitaya.combatnexus.combat.ability.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.ability.api.*;
import java.util.*;

/** Executor declarations are validated before candidate definitions are adopted. */
public final class AbilityExecutorCatalog {
    private static final Map<NexusId, AbilityExecutorSpec> SPECS = new LinkedHashMap<>();
    private static boolean frozen;
    private AbilityExecutorCatalog() {}
    public static synchronized void register(NexusId id, AbilityExecutorSpec spec) {
        if (frozen) throw new IllegalStateException("Ability executors frozen");
        if (SPECS.size() >= 256) throw new IllegalArgumentException("Ability executor capacity exceeded");
        if (SPECS.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(spec)) != null) throw new IllegalArgumentException("Duplicate ability executor: " + id);
    }
    public static synchronized void freeze() { frozen = true; }
    public static AbilityExecutorSpec require(NexusId id) {
        var spec = SPECS.get(Objects.requireNonNull(id));
        if (spec == null) throw new IllegalArgumentException("Unknown ability executor: " + id);
        return spec;
    }
    public static void validate(AbilityDefinition definition) {
        if (!require(definition.executor()).targets().containsAll(definition.targets())) throw new IllegalArgumentException("Unsupported executor targets: " + definition.id());
    }
}
