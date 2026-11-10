package io.github.fatalitaya.combatnexus.combat.internal.persistence;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import java.util.*;

/** Detached persisted progress, independent of the live installation owner. */
public record SavedSource(SourceInstance source, SourcePlan plan, boolean suspended, boolean pending, UUID binding) {}
