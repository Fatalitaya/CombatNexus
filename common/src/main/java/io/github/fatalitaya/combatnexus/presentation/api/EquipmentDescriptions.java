package io.github.fatalitaya.combatnexus.presentation.api;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.entry.*;
import io.github.fatalitaya.combatnexus.combat.api.affix.*;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.api.DomainDefinitions;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorDefinition;
import io.github.fatalitaya.combatnexus.effect.api.*;
import java.util.*;
import java.util.function.*;
/** One descriptor for saved instances and adopted templates; static tooltips never choose equipment winners. */
public final class EquipmentDescriptions {
 private EquipmentDescriptions(){}
 public static DescriptionDocument entry(EntryDefinition entry,BiFunction<EntryContribution.Kind,NexusId,Optional<DescriptionDocument>> references){
  var damage=io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageDescriptions.project(List.of(entry),List.of()).get(0);
  var children=new ArrayList<DescriptionDocument>();
  for(var c:entry.contributions()){
   if(c instanceof EntryContribution.Damage ignored) {}
   if(c instanceof EntryContribution.Attribute a) { children.add(leaf(a.id(),List.of(AttributeDescription.describe(GeneratedTexts.translatable(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().descriptionId(a.attribute())),a.attribute(),new io.github.fatalitaya.combatnexus.attribute.api.AttributeCalculation.Modifier(a.id(),a.amount(),a.operation()))))); }
   if(c instanceof EntryContribution.Behavior a) { children.add(required(references,EntryContribution.Kind.BEHAVIOR,a.definition())); }
   if(c instanceof EntryContribution.Effect a) { children.add(required(references,EntryContribution.Kind.EFFECT,a.definition())); }
   if(c instanceof EntryContribution.Regeneration a) { children.add(required(references,EntryContribution.Kind.REGENERATION,a.definition())); }
   if(c instanceof EntryContribution.Ability a) { children.add(required(references,EntryContribution.Kind.ABILITY,a.definition())); }
   if(c instanceof EntryContribution.Shield a) {var d=required(references,EntryContribution.Kind.SHIELD,a.definition());var lines=new ArrayList<>(d.generated());lines.add(text("shield_grant",a.amount(),a.durationTicks()));children.add(new DescriptionDocument(d.title(),d.authoredSummary(),lines,d.details(),d.flavor(),d.children(),d.rules(),true));}
  }
  return new DescriptionDocument(damage.title(),damage.authoredSummary(),List.of(),damage.details(),damage.flavor(),children,damage.showRuleBreakdown()?damage.rules():List.of(),true);
 }
 public static DescriptionDocument affix(AffixDefinition affix,BiFunction<EntryContribution.Kind,NexusId,Optional<DescriptionDocument>> references,Function<NexusId,Optional<DescriptionDocument>> entries){
  var children=affix.members().stream().map(m->m.entry().map(e->entry(e,references)).orElseGet(()->entries.apply(m.template().orElseThrow()).orElseThrow(()->new IllegalArgumentException("Missing member description")))).toList();
  var summary=affix.members().stream().allMatch(m->m.entry().isPresent())?io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageDescriptions.project(List.of(),List.of(affix)).get(0).authoredSummary():affix.display().authoredSummary();
  return new DescriptionDocument(affix.display().name(),summary,List.of(text("rarity."+affix.rarity().serializedName())),List.of(GeneratedTexts.translatable("tooltip.combatnexus.stacking."+affix.stacking().serializedName())),affix.display().flavorText(),children,List.of(),true);
 }
 public static DescriptionDocument binding(CombatItemBindingSpec.Binding binding,BiFunction<EntryContribution.Kind,NexusId,Optional<DescriptionDocument>> references,Function<NexusId,Optional<DescriptionDocument>> entries,Function<NexusId,Optional<DescriptionDocument>> affixes){
  DescriptionDocument d=binding.kind()==CombatItemBindingSpec.Kind.ENTRY?binding.entry().map(e->entry(e,references)).orElseGet(()->entries.apply(binding.template().orElseThrow()).orElseThrow()):binding.affix().map(a->affix(a,references,entries)).orElseGet(()->affixes.apply(binding.template().orElseThrow()).orElseThrow());
  var details=new ArrayList<>(d.details());details.add(text("slots",String.join(", ",binding.slots().stream().map(s->s.getName()).toList())));
  return new DescriptionDocument(d.title(),d.authoredSummary(),d.generated(),details,d.flavor(),d.children(),d.rules(),d.showRuleBreakdown());
 }
 private static DescriptionDocument required(BiFunction<EntryContribution.Kind,NexusId,Optional<DescriptionDocument>> refs,EntryContribution.Kind kind,NexusId id){return refs.apply(kind,id).orElseThrow(()->new IllegalArgumentException("Missing contribution description: "+id));}
 public static Optional<DescriptionDocument> reference(DomainDefinitions domains,Map<NexusId,BehaviorDefinition> behaviors,EntryContribution.Kind kind,NexusId id){return switch(kind){
  case EFFECT -> Optional.ofNullable(domains.effects().get(id)).map(e->DescriptionDocument.leaf(EffectTooltip.describe(id,e)));
  case BEHAVIOR -> Optional.ofNullable(behaviors.get(id)).map(e->DescriptionDocument.leaf(EffectTooltip.describe(id,new EffectDefinition(0,e.lifetime(),EffectService.Stacking.REPLACE,EffectPolicy.DEFAULT,List.of(),e.plan(),Set.of()))));
  case SHIELD -> Optional.ofNullable(domains.shields().get(id)).map(s->leaf(id,List.of(text("shield",s.capacity(),s.recoveryPerSecond(),s.recoveryDelayTicks()),text("shield_filters",s.damageTypes().toString(),s.damageTypeTags().toString(),s.channels().toString()))));
  case REGENERATION -> Optional.ofNullable(domains.regeneration().get(id)).map(r->leaf(id,List.of(text("regeneration",r.healthPerSecond(),r.damageDelayTicks(),r.durationTicks()),text("regeneration_conditions",Boolean.toString(r.respectsNaturalRegenerationRule()),Boolean.toString(r.requiresOutOfCombat())))));
  case ABILITY -> Optional.ofNullable(domains.abilities().get(id)).map(AbilityDescriptions::describe);
  default -> Optional.empty();
 };}
 private static DescriptionDocument leaf(NexusId id,List<DisplayText> lines){return new DescriptionDocument(Optional.of(GeneratedTexts.translatableWithFallback("entry."+id.namespace()+"."+id.path().replace('/','.'),id.path())),List.of(),lines,List.of(),Optional.empty(),List.of(),List.of(),true);}
 private static DisplayText text(String key,Object... args){return GeneratedTexts.translatable("tooltip.combatnexus.equipment."+key,args);}
}
