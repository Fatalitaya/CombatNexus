package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import io.github.fatalitaya.combatnexus.combat.internal.persistence.SavedEquipment;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.api.behavior.EquipmentItem;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import io.github.fatalitaya.combatnexus.combat.internal.definition.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.combat.api.platform.EquipmentObservation;
import java.util.*;

/** Binding lifetimes own stable contribution identities, including dormant and consumed grants. */
public final class EquipmentBehaviorController {
 private static final NexusId PROVIDER=new NexusId("combatnexus","equipment");
 private static final class Installed {
  final NexusId item;CombatItemBindingSpec.Binding binding;final UUID installation;
  final Map<NexusId,SourcePlan> contributions=new LinkedHashMap<>();boolean selected;
  Installed(NexusId item,CombatItemBindingSpec.Binding binding,UUID installation){this.item=item;this.binding=binding;this.installation=installation;}
 }
 private final CombatRuntime world;private final EquipmentObservation observations;
 private final Map<UUID,Map<EquipmentSelection.Key,Installed>> installed=new LinkedHashMap<>();
 private final Map<UUID,EquipmentSelection> selections=new HashMap<>();
 private final Set<UUID> tracked=new LinkedHashSet<>();
 public EquipmentBehaviorController(CombatRuntime world,EquipmentObservation observations){this.world=world;this.observations=Objects.requireNonNull(observations);}
 public void track(UUID target){CombatMutationGuard.requireWorldWritePhase();if(tracked.size()>=4096&&!tracked.contains(target))throw new IllegalStateException("Equipment target budget exceeded");tracked.add(target);}
 private SourceInstance source(UUID target,EquipmentSelection.Key key,Installed value,NexusId contribution){
  String path=key.slot().getName()+"/"+key.binding()+"/"+contribution;
  UUID instance=EquipmentSelection.instance(value.installation,key,contribution);
  return new SourceInstance(instance,PROVIDER,Optional.of(target),path);
 }
 private boolean revoke(UUID target,EquipmentSelection.Key key,Installed value,NexusId id){
  var handle=world.sourceInstallations().handle(target,source(target,key,value,id).instance());
  if(handle.isPresent()&&!world.sourceInstallations().revoke(UUID.randomUUID(),handle.get()).applied())return false;
  value.contributions.remove(id);return true;
 }
 public void changed(UUID target,EquipmentPosition slot,EquipmentItem current){track(target);refresh(target,Map.of(slot,current));}
 private void refresh(UUID target,Map<EquipmentPosition,EquipmentItem> overrides){
  CombatMutationGuard.requireWorldWritePhase();if(!observations.alive(target))return;
  var observed=new EnumMap<EquipmentPosition,EquipmentItem>(EquipmentPosition.class);
  for(var slot:EquipmentPosition.values())observations.item(target,slot).ifPresent(item->observed.put(slot,overrides.getOrDefault(slot,item)));
  var desired=EquipmentSelection.resolve(observed,world.definitions());
  boolean reloaded=selections.containsKey(target)&&selections.get(target).generation()!=desired.generation();
  var ledger=installed.computeIfAbsent(target,ignored->new LinkedHashMap<>());
  var candidates=new LinkedHashMap<EquipmentSelection.Key,EquipmentSelection.Candidate>();desired.candidates().forEach(c->candidates.put(c.key(),c));
  var confirmed=new LinkedHashSet<EquipmentSelection.Key>();RuntimeException failure=null;
  for(var entry:List.copyOf(ledger.entrySet())){
   var candidate=candidates.get(entry.getKey());var value=entry.getValue();
   if(candidate==null||!value.item.equals(candidate.item())||(!value.binding.equals(candidate.definition().binding())&&(!reloaded||value.binding.kind()!=candidate.definition().binding().kind()))){
    boolean complete=true;for(var id:List.copyOf(value.contributions.keySet()))complete&=revoke(target,entry.getKey(),value,id);
    if(complete)ledger.remove(entry.getKey());else {candidates.remove(entry.getKey());failure=new IllegalStateException("Equipment cleanup pending");}
   }
  }
  // Suspend losers before activating winners, so transient overlap cannot execute benefits twice.
  for(var entry:ledger.entrySet())if(!desired.selected().contains(entry.getKey())){
   entry.getValue().selected=false;
   for(var id:entry.getValue().contributions.keySet()){
    var handle=world.sourceInstallations().handle(target,source(target,entry.getKey(),entry.getValue(),id).instance());
    if(handle.isPresent()&&!world.sourceInstallations().suspend(UUID.randomUUID(),handle.get(),true).applied())failure=new IllegalStateException("Equipment suspension pending");
   }
  }
  for(var candidate:candidates.values()){
   var key=candidate.key();var value=ledger.computeIfAbsent(key,ignored->new Installed(candidate.item(),candidate.definition().binding(),UUID.randomUUID()));
   value.binding=candidate.definition().binding();
   var plans=new LinkedHashMap<NexusId,SourcePlan>();candidate.definition().contributions().forEach(c->plans.put(c.id(),provenance(c.plan(),target,candidate)));
   boolean complete=true;
   for(var id:List.copyOf(value.contributions.keySet()))if(!plans.containsKey(id))complete&=revoke(target,key,value,id);
   boolean selected=desired.selected().contains(key);
   for(var entry:plans.entrySet()){
    var origin=source(target,key,value,entry.getKey());var handle=world.sourceInstallations().handle(target,origin.instance());
    var previous=value.contributions.get(entry.getKey());
    if(handle.isPresent()&&previous!=null&&!sameParts(previous,entry.getValue())){
     if(!revoke(target,key,value,entry.getKey())){complete=false;continue;}
     handle=Optional.empty();
    }
    if(handle.isPresent()){
     if(!entry.getValue().equals(value.contributions.get(entry.getKey()))){
      var updated=world.sourceInstallations().updateEquipment(UUID.randomUUID(),handle.get(),entry.getValue());
      if(updated.applied())value.contributions.put(entry.getKey(),entry.getValue());else complete=false;
     }
     if(!world.sourceInstallations().suspend(UUID.randomUUID(),handle.get(),!selected).applied())complete=false;
    }else if(selected){
     value.contributions.put(entry.getKey(),entry.getValue());
     complete&=world.sourceInstallations().install(new SourceInstallation(UUID.randomUUID(),target,origin,entry.getValue())).applied();
    }
   }
   value.selected=selected&&complete;
   if(value.selected)confirmed.add(key);
  }
  selections.put(target,new EquipmentSelection(desired.generation(),desired.candidates().stream().map(c->new EquipmentSelection.Candidate(c.key(),c.item(),c.definition(),ledger.containsKey(c.key())?ledger.get(c.key()).installation:null)).toList(),confirmed));
  if(failure!=null)throw failure;
 }
 private static boolean sameParts(SourcePlan before,SourcePlan after){return before.effects().keySet().equals(after.effects().keySet())&&before.shields().keySet().equals(after.shields().keySet())&&before.regeneration().keySet().equals(after.regeneration().keySet())&&before.abilities().equals(after.abilities());}
 private static SourcePlan provenance(SourcePlan plan,UUID target,EquipmentSelection.Candidate candidate){
  var effects=new LinkedHashMap<NexusId,io.github.fatalitaya.combatnexus.effect.api.EffectDefinition>();
  plan.effects().forEach((id,e)->effects.put(id,new io.github.fatalitaya.combatnexus.effect.api.EffectDefinition(e.level(),e.lifetime(),e.stacking(),e.policy(),e.attributes(),e.behavior(),e.restrictions(),e.display().withProvenance(new SourceProvenance(target,candidate.item(),candidate.key().slot(),null,null)))));
  return new SourcePlan(plan.definition(),effects,plan.regeneration(),plan.shields(),plan.abilities(),plan.persistent());
 }
 public EquipmentSelection selection(UUID target){return selections.getOrDefault(target,new EquipmentSelection(world.definitions().generation(),List.of(),Set.of()));}
 public void tick(long generation){CombatMutationGuard.requireWorldWritePhase();for(var target:List.copyOf(tracked))try{refresh(target,Map.of());}catch(RuntimeException failure){/* Retain ownership and retry on the next lifecycle opportunity. */}}
 public record EquipmentView(UUID installation, EquipmentPosition slot, NexusId binding, NexusId definition, boolean selected) {}
 public List<EquipmentView> projection(UUID target) {
  return installed.getOrDefault(target,Map.of()).entrySet().stream().map(e -> new EquipmentView(
      e.getValue().installation,e.getKey().slot(),e.getValue().binding.id(),e.getValue().binding.definition(),e.getValue().selected)).toList();
 }
 public List<io.github.fatalitaya.combatnexus.combat.api.item.EquipmentBindingFact> facts(UUID target) {
  world.requireActive();
  var evidence=world.sourceInstallations().snapshot(target);
  return installed.getOrDefault(target,Map.of()).entrySet().stream().map(entry->{
   var value=entry.getValue();var instances=value.contributions.keySet().stream().map(id->source(target,entry.getKey(),value,id).instance()).collect(java.util.stream.Collectors.toSet());
   return new io.github.fatalitaya.combatnexus.combat.api.item.EquipmentBindingFact(value.installation,entry.getKey().slot(),value.item,value.binding,value.selected,
    evidence.stream().filter(view->instances.contains(view.source().instance())).toList(),world.abilities().grants(target).stream().filter(grant->instances.contains(grant.source().instance())).toList());
  }).toList();
 }

 public List<SavedEquipment> save(UUID target){return installed.getOrDefault(target,Map.of()).entrySet().stream().map(e->new SavedEquipment(e.getKey().slot(),e.getValue().item,e.getValue().binding,e.getValue().installation,e.getValue().contributions,e.getValue().selected)).toList();}
 public Set<NexusId> sources(UUID target){var result=new HashSet<NexusId>();installed.getOrDefault(target,Map.of()).forEach((key,value)->value.contributions.keySet().forEach(id->result.add(SourceInstallations.key(source(target,key,value,id)))));return Set.copyOf(result);}
 public Set<NexusId> restore(UUID target,List<SavedEquipment> saved){
  CombatMutationGuard.requireWorldWritePhase();var ledger=installed.computeIfAbsent(target,ignored->new LinkedHashMap<>());
  for(var entry:saved){var observed=observations.item(target,entry.slot());if(observed.isEmpty()||!observed.get().item().equals(entry.item())||!observed.get().bindings().bindings().contains(entry.binding()))continue;
   var value=new Installed(entry.item(),entry.binding(),entry.installation());value.contributions.putAll(entry.contributions());value.selected=entry.selected();
   if(ledger.putIfAbsent(new EquipmentSelection.Key(entry.slot(),entry.binding().id()),value)!=null)throw new IllegalArgumentException("Duplicate saved equipment binding");
  }
  track(target);return sources(target);
 }
 public void restored(UUID target){refresh(target,Map.of());}
 public void clear(UUID target){tracked.remove(target);installed.remove(target);selections.remove(target);}
 public void close(){tracked.clear();installed.clear();selections.clear();}
}
