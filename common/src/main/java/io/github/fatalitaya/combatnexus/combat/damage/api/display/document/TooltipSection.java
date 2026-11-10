package io.github.fatalitaya.combatnexus.combat.damage.api.display.document;

public sealed interface TooltipSection permits
        AffixTooltipView,
        EntryTooltipView,
        VanillaTooltipAugmentation {
}
