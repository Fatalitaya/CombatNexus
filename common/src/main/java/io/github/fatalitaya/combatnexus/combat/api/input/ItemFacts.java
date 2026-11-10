package io.github.fatalitaya.combatnexus.combat.api.input;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.item.RuleItemView;
import java.util.*;

/** Detached combat component projection. Never retains a native stack. */
public record ItemFacts(boolean isEmpty, NexusId itemId, int count, CombatItemBindingSpec defaultBindings,
        CombatItemBindingPatch bindingPatch, Optional<List<EntryDefinition>> entries,
        Optional<List<AffixDefinition>> affixes) implements RuleItemView {
    public ItemFacts {
        Objects.requireNonNull(itemId); Objects.requireNonNull(defaultBindings); Objects.requireNonNull(bindingPatch);
        entries = entries.map(List::copyOf); affixes = affixes.map(List::copyOf);
    }
}
