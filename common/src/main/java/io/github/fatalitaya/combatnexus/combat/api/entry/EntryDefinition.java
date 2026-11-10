package io.github.fatalitaya.combatnexus.combat.api.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import java.util.*;

public record EntryDefinition(NexusId id, EntryDisplay display, List<EntryContribution> contributions,
                              EntryStacking stacking, Optional<NexusId> stackingGroup) {
    private static final Codec<EntryDefinition> STRUCTURAL_CODEC = RecordCodecBuilder.create(i->i.group(
        NexusIdCodecs.CODEC.fieldOf("id").forGetter(EntryDefinition::id),EntryDisplay.CODEC.fieldOf("display").forGetter(EntryDefinition::display),
        EntryContribution.CODEC.listOf(1,64).fieldOf("contributions").forGetter(EntryDefinition::contributions),
        EntryStacking.CODEC.optionalFieldOf("stacking",EntryStacking.STACK).forGetter(EntryDefinition::stacking),
        NexusIdCodecs.CODEC.optionalFieldOf("stacking_group").forGetter(EntryDefinition::stackingGroup)).apply(i,EntryDefinition::new));
    public static final Codec<EntryDefinition> CODEC=io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinitionCodec.checked(STRUCTURAL_CODEC,"combatnexus_entry_templates");
    public static final Codec<EntryDefinition> STORAGE_CODEC = CODEC;
    public EntryDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(display); Objects.requireNonNull(stacking); Objects.requireNonNull(stackingGroup);
        contributions=List.copyOf(contributions);
        if(id.toString().length()>256 || contributions.isEmpty()||contributions.size()>64||contributions.stream().map(EntryContribution::id).distinct().count()!=contributions.size())
            throw new IllegalArgumentException("Entry contribution budget or duplicate identity: "+id);
        for(var c:contributions) if(c.id().toString().length()>96) throw new IllegalArgumentException("Contribution identifier too long");
    }
    public NexusId stackingKey(){return stackingGroup.orElse(id);}
    public List<DamageRuleDefinition> rules(){return contributions.stream().filter(EntryContribution.Damage.class::isInstance).map(EntryContribution.Damage.class::cast).map(EntryContribution.Damage::rule).toList();}
}
