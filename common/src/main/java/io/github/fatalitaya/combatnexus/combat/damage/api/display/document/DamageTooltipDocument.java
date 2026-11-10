package io.github.fatalitaya.combatnexus.combat.damage.api.display.document;

import java.util.List;

public record DamageTooltipDocument(
        List<AffixTooltipView> affixes,
        List<EntryTooltipView> standaloneEntries,
        List<VanillaTooltipAugmentation> vanillaAugmentations
) {
    public DamageTooltipDocument {
        affixes = affixes == null ? List.of() : List.copyOf(affixes);
        standaloneEntries = standaloneEntries == null ? List.of() : List.copyOf(standaloneEntries);
        vanillaAugmentations = vanillaAugmentations == null ? List.of() : List.copyOf(vanillaAugmentations);
    }

    public boolean isEmpty() {
        return affixes.isEmpty()
                && standaloneEntries.isEmpty()
                && vanillaAugmentations.isEmpty();
    }
}
