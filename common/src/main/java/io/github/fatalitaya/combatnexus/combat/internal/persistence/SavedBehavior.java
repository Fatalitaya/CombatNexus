package io.github.fatalitaya.combatnexus.combat.internal.persistence;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import java.util.*;

/** Detached persisted progress, independent of the live installation owner. */
public record SavedBehavior(NexusId effect, NexusId source, Map<NexusId, NexusId> shieldPools, Map<NexusId, Integer> consumed, Map<NexusId, Long> protectionRemaining, Map<NexusId, Long> periodicRemaining, Map<String, Integer> triggers) {
        public SavedBehavior {
            Objects.requireNonNull(effect); Objects.requireNonNull(source); shieldPools = Map.copyOf(shieldPools); consumed = Map.copyOf(consumed); periodicRemaining = Map.copyOf(periodicRemaining);
            triggers = Map.copyOf(triggers); protectionRemaining = Map.copyOf(protectionRemaining);
            if (protectionRemaining.size() > 64 || !consumed.keySet().containsAll(protectionRemaining.keySet()) || protectionRemaining.values().stream().anyMatch(v -> v < 0 || v > 1200000)) throw new IllegalArgumentException("Invalid protection cooldown");
            if (periodicRemaining.values().stream().anyMatch(v -> v < 0 || v > 1200000) || triggers.size() > 64
                    || triggers.values().stream().anyMatch(v -> v < 0 || v > 64))
                throw new IllegalArgumentException("Invalid saved behavior progress");
        }
    }
