package io.github.fatalitaya.combatnexus.combat.damage.api.display;

import io.github.fatalitaya.combatnexus.presentation.api.TooltipNumbers;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Composable names and quantities. Native types and damage channels remain distinct identities. */
public final class DamageText {
    private DamageText() {}

    public static DisplayText type(NexusId id) {
        return named("damage_type.", id);
    }

    public static DisplayText channel(NexusId id) {
        return named("damage_channel.", id);
    }

    public static DisplayText kind(DisplayText name) {
        return GeneratedTexts.translatable("tooltip.combatnexus.damage.kind", name);
    }

    public static DisplayText amount(double amount, DisplayText name) {
        return GeneratedTexts.translatable("tooltip.combatnexus.damage.amount", TooltipNumbers.number(amount), kind(name));
    }

    private static DisplayText named(String prefix, NexusId id) {
        return GeneratedTexts.translatableWithFallback(prefix + id.getNamespace() + "." + id.getPath().replace('/', '.'), id.toString());
    }
}
