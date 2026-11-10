package io.github.fatalitaya.combatnexus.combat.damage.core.config;
import io.github.fatalitaya.combatnexus.combat.config.CombatServerSettings;
import io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration;
/** Derived view of the single adopted combat generation; no independent publication. */
public final class DamageSettingsStore {
    public record Snapshot(long revision, CombatServerSettings values) {}
    private DamageSettingsStore() {}
    public static Snapshot current(DefinitionGeneration generation) {
        return new Snapshot(generation.generation(), generation.settings());
    }
}
