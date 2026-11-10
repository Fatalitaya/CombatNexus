package io.github.fatalitaya.combatnexus.combat.internal.persistence;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import java.util.*;

/** Detached persisted progress, independent of the live installation owner. */
public record SavedEquipment(EquipmentPosition slot,NexusId item,CombatItemBindingSpec.Binding binding,UUID installation,Map<NexusId,SourcePlan> contributions,boolean selected){
  public SavedEquipment {Objects.requireNonNull(slot);Objects.requireNonNull(item);Objects.requireNonNull(binding);Objects.requireNonNull(installation);contributions=Collections.unmodifiableMap(new LinkedHashMap<>(contributions));if(contributions.size()>64)throw new IllegalArgumentException("Equipment save budget");}
 }
