package io.github.fatalitaya.combatnexus.presentation.api;

import io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts;

/** Generic rendering of confirmed source facts; contains no vanilla effect rules. */
public final class SourceProvenanceText {
    private SourceProvenanceText() {}
    public static DisplayText describe(SourceProvenance origin) {
        var parts = new java.util.ArrayList<DisplayText>();
        if (origin.entity() != null) parts.add(GeneratedTexts.literal(origin.entity().toString()));
        if (origin.item() != null) parts.add(GeneratedTexts.translatable("tooltip.combatnexus.source.item", origin.item().toString()));
        if (origin.slot() != null) parts.add(GeneratedTexts.translatable("tooltip.combatnexus.source.slot", origin.slot().getName()));
        if (origin.enchantment() != null) parts.add(GeneratedTexts.translatable("tooltip.combatnexus.source.enchantment", origin.enchantment().toString()));
        if (origin.block() != null) {
            var b = origin.block();
            parts.add(GeneratedTexts.translatable("tooltip.combatnexus.source.block", b.dimension().toString(), b.x(), b.y(), b.z()));
        }
        if (parts.isEmpty()) return GeneratedTexts.translatable("tooltip.combatnexus.effect.unknown_source");
        return GeneratedTexts.join(parts, " / ");
    }
}
