package io.github.fatalitaya.combatnexus.combat.damage.api.item;

import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import java.util.List;

/** Resolved server display projection; not an execution capability or an item mutation. */
public record ItemCombatPresentationSnapshot(long generation, List<EntryDefinition> entries,
                                             List<AffixDefinition> affixes) {
    public ItemCombatPresentationSnapshot { entries = List.copyOf(entries); affixes = List.copyOf(affixes); }
}
