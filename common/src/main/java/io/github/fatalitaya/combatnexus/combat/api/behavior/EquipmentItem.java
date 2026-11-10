package io.github.fatalitaya.combatnexus.combat.api.behavior;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;

/** Immutable equipment facts used for installation and reconciliation. */
public record EquipmentItem(NexusId item, CombatItemBindingSpec bindings, java.util.Optional<String> rejection) {
    public EquipmentItem {
        Objects.requireNonNull(item);
        Objects.requireNonNull(bindings); Objects.requireNonNull(rejection);
        if(rejection.isPresent() && !bindings.bindings().isEmpty())throw new IllegalArgumentException("Rejected equipment cannot carry effective bindings");
    }
}
