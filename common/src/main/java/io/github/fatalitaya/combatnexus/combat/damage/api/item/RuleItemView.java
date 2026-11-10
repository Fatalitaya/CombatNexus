package io.github.fatalitaya.combatnexus.combat.damage.api.item;

import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import java.util.List;
import java.util.Optional;

/** Read-only executable rule components for rule evaluation. */
public interface RuleItemView {
    boolean isEmpty();
    Optional<List<EntryDefinition>> entries();
    Optional<List<AffixDefinition>> affixes();
    io.github.fatalitaya.combatnexus.api.id.NexusId itemId();
    CombatItemBindingSpec defaultBindings();
    CombatItemBindingPatch bindingPatch();
}
