package io.github.fatalitaya.combatnexus.presentation.platform;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.presentation.api.*;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Supplements gaps in native tracking. The subject is any living entity, never a player-only model. */
public final class NativeEntityCombatMirror {
    private NativeEntityCombatMirror() {}
    public static JsonObject capture(CombatRuntime runtime, LivingEntity entity) {
        var budget = new PresentationBudget(512, 32000);
        var attributes = runtime.captureAttributes(entity, runtime.attributeCatalog(), budget::entry);
        var panel = runtime.panel(entity, attributes, budget::entry);
        var json = CombatPanelProjector.supplement(panel, entity.level().getGameTime());
        var missing = new JsonObject(); var overrides = new JsonObject();
        for (var holder : runtime.attributeCatalog()) {
            var id = NativeIds.nexus(holder.unwrapKey().orElseThrow().identifier());
            var value = attributes.valueFrame().values().get(id);
            if (value == null) continue;
            if (!holder.value().isClientSyncable()) {
                var entry = new JsonObject();
                var row = EntityAttributeRows.value(id, value.base(), value.effective());
                if (value.raw() != value.effective()) EntityAttributeRows.effective(row, value.raw(), value.effective(), value.effectiveReason());
                entry.add("row", row); entry.add("sources", EntityAttributeRows.sources(attributes.provenance().sources().get(id)));
                missing.add(id.toString(), entry);
            } else if (value.raw() != value.effective()) {
                var row = new JsonObject(); EntityAttributeRows.effective(row, value.raw(), value.effective(), value.effectiveReason());
                overrides.add(id.toString(), row);
            }
        }
        json.addProperty("restrictionMask", runtime.effects().restrictionMask(entity.getUUID()));
        json.add("attributes", missing); json.add("effective", overrides);
        // Player supplies a native synced accessor; base LivingEntity keeps absorption in a local field.
        if (!(entity instanceof Player)) json.addProperty("absorption", entity.getAbsorptionAmount());
        return json;
    }
}
