package io.github.fatalitaya.combatnexus.combat.damage.core.security;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.item.RuleItemView;
import io.github.fatalitaya.combatnexus.combat.internal.definition.*;
import java.util.*;

/** Server-authoritative binding validation. Captured equipment already contains the selected damage facts. */
public final class DamageModuleItemSecurity {
 private DamageModuleItemSecurity(){}
 public record ValidatedItemRules(List<EntryDefinition> entries,List<AffixDefinition> affixes,boolean authoritative){
  public ValidatedItemRules{entries=List.copyOf(entries);affixes=List.copyOf(affixes);}
  public boolean isEmpty(){return entries.isEmpty()&&affixes.isEmpty();}
 }
 public static ValidatedItemRules validateDefinitions(List<EntryDefinition> entries,List<AffixDefinition> affixes,String source){
  var problem=io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleLimits.findItemProblem(entries,affixes);
  if(problem.isPresent())throw new IllegalArgumentException(problem.get());
  return new ValidatedItemRules(entries,affixes,true);
 }
 public static ValidatedItemRules validateForExecution(RuleItemView item,String source,DefinitionGeneration generation){
  if(item==null||item.isEmpty())return new ValidatedItemRules(List.of(),List.of(),true);
  if(item.entries().isPresent()&&item.affixes().isPresent())return new ValidatedItemRules(item.entries().orElseThrow(),item.affixes().orElseThrow(),true);
  return io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry.withCandidate(generation.damage().channels(),()->{
   var entries=new ArrayList<EntryDefinition>();var affixes=new ArrayList<AffixDefinition>();
   for(var binding:generation.bindings(item).bindings()){
    if(binding.kind()==io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec.Kind.ENTRY)entries.add(EquipmentDefinitionCompiler.entry(binding,generation));
    else affixes.add(EquipmentDefinitionCompiler.affix(binding,generation));
   }
   return validateDefinitions(entries,affixes,source);
  });
 }
}
