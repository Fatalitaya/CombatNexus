package io.github.fatalitaya.combatnexus.combat.damage.api.item;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import java.util.Optional;

/** Read-only adopted generation for rule item lookup and authoritative presentation. */
public interface DamageItemDefinitions {
    long generation();
    Optional<EntryDefinition> entry(NexusId id);
    Optional<AffixDefinition> affix(NexusId id);
    boolean templatesReady();
    long templateChannelRevision();
    ItemCombatPresentationSnapshot itemPresentation(RuleItemView item);
}
