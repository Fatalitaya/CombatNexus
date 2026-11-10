package io.github.fatalitaya.combatnexus.combat.api.affix;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.*;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import java.util.*;
public record AffixMember(NexusId id, Optional<NexusId> template, Optional<EntryDefinition> entry) {
    public static final Codec<AffixMember> CODEC=RecordCodecBuilder.create(i->i.group(NexusIdCodecs.CODEC.fieldOf("id").forGetter(AffixMember::id),NexusIdCodecs.CODEC.optionalFieldOf("template").forGetter(AffixMember::template),EntryDefinition.CODEC.optionalFieldOf("entry").forGetter(AffixMember::entry)).apply(i,AffixMember::new));
    public AffixMember { Objects.requireNonNull(id);Objects.requireNonNull(template);Objects.requireNonNull(entry);
        if(id.toString().length()>96 || template.isPresent()==entry.isPresent())throw new IllegalArgumentException("Member requires exactly one template or entry"); }
    public static AffixMember reference(NexusId id,NexusId template){return new AffixMember(id,Optional.of(template),Optional.empty());}
    public static AffixMember inline(NexusId id,EntryDefinition entry){return new AffixMember(id,Optional.empty(),Optional.of(entry));}
    public EntryDefinition resolve(java.util.function.Function<NexusId,Optional<EntryDefinition>> templates){return entry.orElseGet(()->templates.apply(template.orElseThrow()).orElseThrow(()->new IllegalArgumentException("Unknown entry template: "+template.orElseThrow())));}
}
