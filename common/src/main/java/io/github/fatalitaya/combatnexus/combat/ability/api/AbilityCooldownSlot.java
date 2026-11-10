package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Explicit player-owned identity; each command selects exactly one slot. */
public sealed interface AbilityCooldownSlot {
    record Skill(NexusId ability) implements AbilityCooldownSlot {
        public Skill { Objects.requireNonNull(ability); }
    }
    record Contribution(UUID source, NexusId contribution) implements AbilityCooldownSlot {
        public Contribution { Objects.requireNonNull(source); Objects.requireNonNull(contribution); }
    }
    record Shared(NexusId group) implements AbilityCooldownSlot {
        public Shared { Objects.requireNonNull(group); }
    }
}
