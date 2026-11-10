package io.github.fatalitaya.combatnexus.presentation.api;

import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;

/** Native component extraction for the shared equipment/enchantment description interpreter. */
public record ItemEffectObservation(CombatItemBindingSpec equipment, List<Enchantment> enchantments) {
    public ItemEffectObservation { java.util.Objects.requireNonNull(equipment); enchantments = List.copyOf(enchantments); }
    public record Enchantment(Optional<DisplayText> title, int level, JsonObject definition) {
        public Enchantment { java.util.Objects.requireNonNull(title); definition = definition.deepCopy(); }
        @Override public JsonObject definition() { return definition.deepCopy(); }
    }
}
