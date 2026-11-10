package io.github.fatalitaya.combatnexus.combat.api.item;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import java.util.*;
public final class CombatItemBindingBuilder {
 private final List<CombatItemBindingSpec.Binding> bindings=new ArrayList<>();
 public CombatItemBindingBuilder add(CombatItemBindingSpec.Binding value){if(bindings.size()>=64||bindings.stream().anyMatch(b->b.id().equals(value.id())))throw new IllegalArgumentException("Duplicate binding or capacity");bindings.add(value);return this;}
 public CombatItemBindingBuilder entryTemplate(NexusId id,NexusId definition,EquipmentPosition... slots){return add(new CombatItemBindingSpec.Binding(id,CombatItemBindingSpec.Kind.ENTRY,Optional.of(definition),Optional.empty(),Optional.empty(),List.of(slots)));}
 public CombatItemBindingBuilder affixTemplate(NexusId id,NexusId definition,EquipmentPosition... slots){return add(new CombatItemBindingSpec.Binding(id,CombatItemBindingSpec.Kind.AFFIX,Optional.of(definition),Optional.empty(),Optional.empty(),List.of(slots)));}
 public CombatItemBindingBuilder entry(NexusId id,EntryDefinition value,EquipmentPosition... slots){return add(new CombatItemBindingSpec.Binding(id,CombatItemBindingSpec.Kind.ENTRY,Optional.empty(),Optional.of(value),Optional.empty(),List.of(slots)));}
 public CombatItemBindingBuilder affix(NexusId id,AffixDefinition value,EquipmentPosition... slots){return add(new CombatItemBindingSpec.Binding(id,CombatItemBindingSpec.Kind.AFFIX,Optional.empty(),Optional.empty(),Optional.of(value),List.of(slots)));}
 public CombatItemBindingSpec build(){return new CombatItemBindingSpec(bindings);}
}
