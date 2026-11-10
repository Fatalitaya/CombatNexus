package io.github.fatalitaya.combatnexus.presentation.platform;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.presentation.api.*;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.world.entity.LivingEntity;

/** Complete server-owned panel data for the current screen subscription. */
public final class NativeEntityCombatMirror {
    private NativeEntityCombatMirror() {}
    public static JsonObject capture(CombatRuntime runtime, LivingEntity entity) {
        var budget = new PresentationBudget(512, 32000);
        var attributes = runtime.captureAttributes(entity, runtime.attributeCatalog(), budget::entry);
        var panel = runtime.panel(entity, attributes, budget::entry);
        var json = CombatPanelProjector.project(panel, entity.level().getGameTime(),
            effect -> runtime.timeControl().advancing(effect.target(), effect.source().orElse(effect.definition())));
        var capturedAttributes = new JsonObject();
        for (var holder : runtime.attributeCatalog()) {
            var id = NativeIds.nexus(holder.unwrapKey().orElseThrow().location());
            var value = attributes.valueFrame().values().get(id);
            if (value == null) continue;
            var entry = new JsonObject();
            var row = EntityAttributeRows.value(id, value.base(), value.effective());
            if (value.raw() != value.effective()) EntityAttributeRows.effective(row, value.raw(), value.effective(), value.effectiveReason());
            entry.add("row", row); entry.add("sources", EntityAttributeRows.sources(attributes.provenance().sources().get(id)));
            capturedAttributes.add(id.toString(), entry);
        }
        json.addProperty("restrictionMask", runtime.effects().restrictionMask(entity.getUUID()));
        json.add("attributes", capturedAttributes);
        json.addProperty("health", panel.health());
        json.addProperty("absorption", panel.vanillaAbsorption());
        budget.encoded(json);
        return json;
    }
}
