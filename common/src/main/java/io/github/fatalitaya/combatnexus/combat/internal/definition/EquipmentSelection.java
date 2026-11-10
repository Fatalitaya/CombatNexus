package io.github.fatalitaya.combatnexus.combat.internal.definition;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.behavior.EquipmentItem;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntrySelectionResolver;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixSelectionResolver;
import io.github.fatalitaya.combatnexus.combat.config.EquipmentSettings.StackingScope;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.input.ItemFacts;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;

/** Shared ordered resolution for equipment installation, damage capture and source presentation. */
public record EquipmentSelection(long generation,List<Candidate> candidates,Set<Key> selected) {
 public record Key(EquipmentPosition slot,NexusId binding) {}
 public record Candidate(Key key,NexusId item,EquipmentDefinitionCompiler.Compiled definition,UUID installation) {
  public Candidate(Key key,NexusId item,EquipmentDefinitionCompiler.Compiled definition){this(key,item,definition,null);}
 }
 public static UUID instance(UUID installation,Key key,NexusId contribution){return UUID.nameUUIDFromBytes((installation+"/"+key.slot().getName()+"/"+key.binding()+"/"+contribution).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 public EquipmentSelection {candidates=List.copyOf(candidates);selected=Set.copyOf(selected);}
 public static EquipmentSelection resolve(Map<EquipmentPosition,EquipmentItem> equipment,DefinitionGeneration generation){
  var candidates=new ArrayList<Candidate>();
  for(var slot:EquipmentPosition.values()){
   var item=equipment.get(slot);if(item==null)continue;
   if(item.rejection().isPresent())throw new IllegalArgumentException(item.rejection().orElseThrow());
   for(var binding:item.bindings().bindings())if(binding.slots().contains(slot))candidates.add(new Candidate(new Key(slot,binding.id()),item.item(),EquipmentDefinitionCompiler.compile(binding,generation,generation.behaviors(),generation.domains())));
  }
  if(candidates.size()>256)throw new IllegalArgumentException("Equipment candidate budget exceeded");
  var selected=new LinkedHashSet<Key>();
  if(generation.settings().equipment().stackingScope()==StackingScope.ENTITY_EQUIPMENT)select(candidates,selected);
  else for(var slot:EquipmentPosition.values())select(candidates.stream().filter(c->c.key().slot()==slot).toList(),selected);
  int contributions=candidates.stream().mapToInt(c->c.definition().contributions().size()).sum();
  if(contributions>64)throw new IllegalArgumentException("Equipment lifetime contribution budget exceeded");
  return new EquipmentSelection(generation.generation(),candidates,selected);
 }
 private static void select(List<Candidate> candidates,Set<Key> selected){
  EntrySelectionResolver.resolve(candidates.stream().filter(c->c.definition().entry()!=null).toList(),c->c.definition().entry()).forEach(c->selected.add(c.key()));
  AffixSelectionResolver.resolve(candidates.stream().filter(c->c.definition().affix()!=null).toList(),c->c.definition().affix()).forEach(c->selected.add(c.key()));
 }
 public List<Candidate> active(EquipmentPosition slot){return candidates.stream().filter(c->c.key().slot()==slot&&selected.contains(c.key())).toList();}
    public ItemFacts capture(ItemFacts item,EquipmentPosition slot){
        var entries=new ArrayList<EntryDefinition>();var affixes=new ArrayList<AffixDefinition>();
        for(var c:active(slot)){
            if(c.definition().entry()!=null){var entry=damage(c.definition().entry(),c,"entry");if(entry!=null)entries.add(entry);}
            else {var a=c.definition().affix();var members=a.members().stream().map(m->new java.util.AbstractMap.SimpleImmutableEntry<>(m.id(),damage(m.entry().orElseThrow(),c,m.id().namespace()+"/"+m.id().path()))).filter(m->m.getValue()!=null).map(m->io.github.fatalitaya.combatnexus.combat.api.affix.AffixMember.inline(m.getKey(),m.getValue())).toList();
                if(!members.isEmpty())affixes.add(new AffixDefinition(a.id(),a.display(),a.rarity(),members,a.stacking(),a.stackingGroup()));}
        }
        return new ItemFacts(item.isEmpty(), item.itemId(), item.count(), CombatItemBindingSpec.EMPTY,
            io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingPatch.EMPTY, Optional.of(entries), Optional.of(affixes));
    }
    private static EntryDefinition damage(EntryDefinition entry,io.github.fatalitaya.combatnexus.combat.internal.definition.EquipmentSelection.Candidate candidate,String member){
        var damage=entry.contributions().stream().filter(io.github.fatalitaya.combatnexus.combat.api.entry.EntryContribution.Damage.class::isInstance).map(io.github.fatalitaya.combatnexus.combat.api.entry.EntryContribution.Damage.class::cast)
            .<io.github.fatalitaya.combatnexus.combat.api.entry.EntryContribution>map(c->{var key=new NexusId("combatnexus",member+"/"+c.id().namespace()+"/"+c.id().path());var instance=io.github.fatalitaya.combatnexus.combat.internal.definition.EquipmentSelection.instance(Objects.requireNonNull(candidate.installation()),candidate.key(),key);return new io.github.fatalitaya.combatnexus.combat.api.entry.EntryContribution.Damage(new NexusId("combatnexus","source/"+instance),c.rule(),c.source());}).toList();
        return damage.isEmpty()?null:new EntryDefinition(entry.id(),entry.display(),damage,entry.stacking(),entry.stackingGroup());
    }
}
