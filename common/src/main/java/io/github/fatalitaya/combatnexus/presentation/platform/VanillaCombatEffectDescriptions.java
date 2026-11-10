package io.github.fatalitaya.combatnexus.presentation.platform;

import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageText;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.List;

/** Level-aware descriptions of native combat effects. */
public final class VanillaCombatEffectDescriptions {
    private VanillaCombatEffectDescriptions() {}
    private static DisplayText damage(int amount, String type) {
        return DamageText.amount(amount, DamageText.type(NexusId.parse(type)));
    }
    public static List<DisplayText> describe(NexusId id, int amplifier) {
        var base = GeneratedTexts.translatable("tooltip.combatnexus.native_effect." + id.getNamespace() + "." + id.getPath());
        DisplayText value = switch (id.getPath()) {
            case "poison" -> GeneratedTexts.translatable("tooltip.combatnexus.vanilla_combat.periodic_damage", damage(1, "neoforge:poison"), Math.max(1, 25 >> amplifier), 1);
            case "wither" -> GeneratedTexts.translatable("tooltip.combatnexus.vanilla_combat.periodic_damage", damage(1, "minecraft:wither"), Math.max(1, 40 >> amplifier), 0);
            case "regeneration" -> GeneratedTexts.translatable("tooltip.combatnexus.vanilla_combat.periodic_healing", 1, Math.max(1, 50 >> amplifier));
            case "instant_health" -> GeneratedTexts.translatable("tooltip.combatnexus.vanilla_combat.instant_health", 4 << amplifier, damage(6 << amplifier, "minecraft:magic"));
            case "instant_damage" -> GeneratedTexts.translatable("tooltip.combatnexus.vanilla_combat.instant_damage", damage(6 << amplifier, "minecraft:magic"), 4 << amplifier);
            default -> null;
        };
        return value == null ? List.of(base) : List.of(base, value);
    }
}
