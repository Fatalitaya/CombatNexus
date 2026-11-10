package io.github.fatalitaya.combatnexus.combat.api.source;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.effect.api.EffectDefinition;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationDefinition;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldDefinition;

/** One installation with independently owned contributions. Definitions are copied at installation. */
public record SourcePlan(NexusId definition, Map<NexusId, EffectDefinition> effects,
        Map<NexusId, RegenerationDefinition> regeneration, Map<NexusId, Shield> shields, Map<NexusId, NexusId> abilities, boolean persistent) {
    public enum ShieldRemoval { REVOKE, KEEP_UNTIL_EXPIRY }
    public record Shield(ShieldDefinition definition, double amount, int durationTicks, ShieldRemoval removal) {
        public Shield {
            Objects.requireNonNull(definition); Objects.requireNonNull(removal);
            if (!Double.isFinite(amount) || amount < 0 || amount > definition.capacity() || durationTicks < 1 || durationTicks > 1200000)
                throw new IllegalArgumentException("Invalid source shield");
        }
    }
    public SourcePlan {
        Objects.requireNonNull(definition); effects = Collections.unmodifiableMap(new LinkedHashMap<>(effects)); regeneration = Collections.unmodifiableMap(new LinkedHashMap<>(regeneration)); shields = Collections.unmodifiableMap(new LinkedHashMap<>(shields));
        abilities = Collections.unmodifiableMap(new LinkedHashMap<>(abilities));
        abilities.forEach((id, definitionId) -> { Objects.requireNonNull(id); Objects.requireNonNull(definitionId); });
        if (effects.size() + regeneration.size() + shields.size() + abilities.size() > 64) throw new IllegalArgumentException("Source contribution capacity exceeded");
    }
}
