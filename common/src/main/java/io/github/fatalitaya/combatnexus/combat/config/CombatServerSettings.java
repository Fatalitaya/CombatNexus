package io.github.fatalitaya.combatnexus.combat.config;

import com.google.gson.JsonObject;
import io.github.fatalitaya.combatnexus.combat.damage.config.VanillaCompatibilitySettings;
import io.github.fatalitaya.combatnexus.combat.damage.config.VanillaReductionCompatibilityMode;
import io.github.fatalitaya.combatnexus.config.SettingsJson;
import io.github.fatalitaya.combatnexus.formula.CombatFormulaSettings;
import java.util.Objects;

/** One validated server rules snapshot; local logging and UI preferences are separate. */
public record CombatServerSettings(CombatFormulaSettings formulas,
                                   VanillaCompatibilitySettings vanillaCompatibility,
                                   CombatSafetySettings safety, EquipmentSettings equipment) {
    public CombatServerSettings {
        Objects.requireNonNull(equipment); Objects.requireNonNull(formulas); Objects.requireNonNull(vanillaCompatibility); Objects.requireNonNull(safety);
        Objects.requireNonNull(vanillaCompatibility.mode());
    }
    public static CombatServerSettings decode(JsonObject formulas, JsonObject damage) {
        SettingsJson.fields(formulas, "asymptotic_k", "resistance_k", "rating_per_protection_score");
        damage=damage.deepCopy();
        if(!damage.has("equipment")){var equipment=new JsonObject();equipment.addProperty("stacking_scope","ENTITY_EQUIPMENT");damage.add("equipment",equipment);}
        SettingsJson.fields(damage, "vanilla_reductions", "limits", "equipment");
        var equipment=SettingsJson.fields(damage.getAsJsonObject("equipment"),"stacking_scope");
        JsonObject vanilla = SettingsJson.fields(damage.getAsJsonObject("vanilla_reductions"),
                "mode", "suppress_armor", "suppress_enchantments", "suppress_mob_effects", "suppress_innate_resistance");
        JsonObject limits = SettingsJson.fields(damage.getAsJsonObject("limits"),
                "max_recursion_depth", "max_derived_requests_per_root", "max_managed_requests_per_server_tick");
        return new CombatServerSettings(
                new CombatFormulaSettings(SettingsJson.number(formulas, "asymptotic_k"),
                        SettingsJson.number(formulas, "resistance_k"), SettingsJson.number(formulas, "rating_per_protection_score")),
                new VanillaCompatibilitySettings(SettingsJson.enumeration(vanilla, "mode", VanillaReductionCompatibilityMode.class),
                        SettingsJson.bool(vanilla, "suppress_armor"), SettingsJson.bool(vanilla, "suppress_enchantments"),
                        SettingsJson.bool(vanilla, "suppress_mob_effects"), SettingsJson.bool(vanilla, "suppress_innate_resistance")),
                new CombatSafetySettings(SettingsJson.integer(limits, "max_recursion_depth"),
                        SettingsJson.integer(limits, "max_derived_requests_per_root"),
                        SettingsJson.integer(limits, "max_managed_requests_per_server_tick")),new EquipmentSettings(SettingsJson.enumeration(equipment,"stacking_scope",EquipmentSettings.StackingScope.class)));
    }
}
