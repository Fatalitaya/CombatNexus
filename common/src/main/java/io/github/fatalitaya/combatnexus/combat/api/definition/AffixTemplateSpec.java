package io.github.fatalitaya.combatnexus.combat.api.definition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.*;
import java.util.*;
public record AffixTemplateSpec(AffixDefinition definition) {
 public AffixTemplateSpec {Objects.requireNonNull(definition);}
 public static final class Builder {
  private final NexusId id;private AffixDisplay display=AffixDisplay.EMPTY;private AffixStacking stacking=AffixStacking.STACK;private Optional<NexusId> group=Optional.empty();private AffixRarity rarity=AffixRarity.COMMON;private final List<AffixMember> members=new ArrayList<>();
  public Builder(NexusId id){this.id=Objects.requireNonNull(id);}
  public Builder display(AffixDisplay value){display=value;return this;}
  public Builder stacking(AffixStacking value){stacking=value;return this;}
  public Builder stackingGroup(NexusId value){group=Optional.of(value);return this;}
  public Builder rarity(AffixRarity value){rarity=value;return this;}
  public Builder member(AffixMember value){if(members.size()>=32)throw new IllegalArgumentException("Affix member budget");members.add(value);return this;}
  public Builder entry(NexusId member,EntryDefinition value){return member(AffixMember.inline(member,value));}
  public Builder entryTemplate(NexusId member,NexusId template){return member(AffixMember.reference(member,template));}
  public AffixTemplateSpec build(){return new AffixTemplateSpec(new AffixDefinition(id,display,rarity,members,stacking,group));}
 }
}
