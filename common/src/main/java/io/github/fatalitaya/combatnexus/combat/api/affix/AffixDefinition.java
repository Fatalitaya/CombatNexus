package io.github.fatalitaya.combatnexus.combat.api.affix;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.*;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import java.util.*;
public record AffixDefinition(NexusId id,AffixDisplay display,AffixRarity rarity,List<AffixMember> members,AffixStacking stacking,Optional<NexusId> stackingGroup) {
    private static final Codec<AffixDefinition> STRUCTURAL_CODEC=RecordCodecBuilder.create(i->i.group(
        NexusIdCodecs.CODEC.fieldOf("id").forGetter(AffixDefinition::id),AffixDisplay.CODEC.fieldOf("display").forGetter(AffixDefinition::display),
        AffixRarity.CODEC.optionalFieldOf("rarity",AffixRarity.COMMON).forGetter(AffixDefinition::rarity),AffixMember.CODEC.listOf(1,32).fieldOf("members").forGetter(AffixDefinition::members),
        AffixStacking.CODEC.optionalFieldOf("stacking",AffixStacking.STACK).forGetter(AffixDefinition::stacking),NexusIdCodecs.CODEC.optionalFieldOf("stacking_group").forGetter(AffixDefinition::stackingGroup)).apply(i,AffixDefinition::new));
    public static final Codec<AffixDefinition> CODEC=io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinitionCodec.checked(STRUCTURAL_CODEC,"combatnexus_affix_templates");
    public static final Codec<AffixDefinition> STORAGE_CODEC=CODEC;
    public AffixDefinition {Objects.requireNonNull(id);Objects.requireNonNull(display);Objects.requireNonNull(rarity);Objects.requireNonNull(stacking);Objects.requireNonNull(stackingGroup);members=List.copyOf(members);
        if(id.toString().length()>256||members.isEmpty()||members.size()>32||members.stream().map(AffixMember::id).distinct().count()!=members.size())throw new IllegalArgumentException("Affix member budget or duplicate identity");}
    public NexusId stackingKey(){return stackingGroup.orElse(id);}
    public AffixDefinition resolve(java.util.function.Function<NexusId,Optional<EntryDefinition>> templates){return new AffixDefinition(id,display,rarity,members.stream().map(m->AffixMember.inline(m.id(),m.resolve(templates))).toList(),stacking,stackingGroup);}
    public List<EntryDefinition> entries(){return members.stream().map(m->m.entry().orElseThrow(()->new IllegalStateException("Affix references have not compiled"))).toList();}
}
