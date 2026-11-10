package io.github.fatalitaya.combatnexus.combat.api.definition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.entry.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter;
public record EntryTemplateSpec(EntryDefinition definition) {
 public EntryTemplateSpec {Objects.requireNonNull(definition);}
 public static final class Builder {
  private final NexusId id; private EntryDisplay display=EntryDisplay.EMPTY;private EntryStacking stacking=EntryStacking.STACK;private Optional<NexusId> group=Optional.empty();private final List<EntryContribution> contributions=new ArrayList<>();
  public Builder(NexusId id){this.id=Objects.requireNonNull(id);}
  public Builder display(EntryDisplay value){display=value;return this;}
  public Builder stacking(EntryStacking value){stacking=value;return this;}
  public Builder stackingGroup(NexusId value){group=Optional.of(value);return this;}
  public Builder contribution(EntryContribution value){if(contributions.size()>=64)throw new IllegalArgumentException("Entry contribution budget");contributions.add(value);return this;}
  public Builder rule(NexusId id,DamageRuleDefinition rule,DamageRuleSourceFilter source){return contribution(new EntryContribution.Damage(id,rule,source));}
  public Builder attribute(NexusId id,NexusId attribute,double amount,AttributeOperation operation){return contribution(new EntryContribution.Attribute(id,attribute,amount,operation));}
  public Builder behavior(NexusId id,NexusId definition){return contribution(new EntryContribution.Behavior(id,definition));}
  public Builder effect(NexusId id,NexusId definition){return contribution(new EntryContribution.Effect(id,definition));}
  public Builder shield(NexusId id,NexusId definition,double amount,int ticks){return contribution(new EntryContribution.Shield(id,definition,amount,ticks));}
  public Builder regeneration(NexusId id,NexusId definition){return contribution(new EntryContribution.Regeneration(id,definition));}
  public Builder ability(NexusId id,NexusId definition){return contribution(new EntryContribution.Ability(id,definition));}
  public EntryTemplateSpec build(){return new EntryTemplateSpec(new EntryDefinition(id,display,contributions,stacking,group));}
 }
}
