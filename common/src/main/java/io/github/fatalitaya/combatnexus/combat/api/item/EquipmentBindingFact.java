package io.github.fatalitaya.combatnexus.combat.api.item;

import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceView;
import java.util.*;

/** Current reconciled binding, including saved instance values and source evidence; no modifier recomputation. */
public record EquipmentBindingFact(UUID installation, EquipmentPosition slot, NexusId item,
        CombatItemBindingSpec.Binding binding, boolean selected, List<SourceView> sources, List<io.github.fatalitaya.combatnexus.combat.ability.api.AbilityGrant> grants) {
    public EquipmentBindingFact {
        Objects.requireNonNull(installation); Objects.requireNonNull(slot); Objects.requireNonNull(item); Objects.requireNonNull(binding);
        sources = List.copyOf(sources); grants = List.copyOf(grants);
    }
}
