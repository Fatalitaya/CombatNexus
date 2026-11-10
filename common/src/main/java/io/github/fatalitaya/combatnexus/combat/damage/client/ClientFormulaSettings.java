package io.github.fatalitaya.combatnexus.combat.damage.client;

import java.util.Optional;

/** Display-only cache for the current connection. No local formula defaults impersonate server settings. */
public final class ClientFormulaSettings {
    public record Settings(long revision, io.github.fatalitaya.combatnexus.formula.CombatFormulaSettings formulas, boolean managedArmor, boolean managedEnchantments) {}
    private static Settings current;
    private ClientFormulaSettings() {}
    public static void accept(Settings payload) {
        if (current == null || payload.revision() >= current.revision()) current = payload;
    }
    public static Optional<Settings> current() { return Optional.ofNullable(current); }
    public static void clear() { current = null; }
}
