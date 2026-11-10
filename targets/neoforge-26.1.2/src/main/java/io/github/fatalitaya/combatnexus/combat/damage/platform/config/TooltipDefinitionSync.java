package io.github.fatalitaya.combatnexus.combat.damage.platform.config;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions;
import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.presentation.api.*;
import io.github.fatalitaya.combatnexus.presentation.platform.network.TooltipDefinitionPayload;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import java.util.*;

/** Projects each adopted shared definition once per login/reload event, independent of any inventory. */
final class TooltipDefinitionSync {
    private TooltipDefinitionSync() {}
    static List<TooltipDefinitionPayload> capture(MinecraftServer server) {
        var definitions = CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (server)));
        var result = new ArrayList<TooltipDefinitionPayload>();
        long generation = definitions.generation();
        definitions.itemBindings().forEach((id,value)->result.add(encode(generation,TooltipDefinitionPayload.Kind.BINDINGS,id,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec.CODEC,JsonOps.INSTANCE,()->value)));
        var templates = definitions.damage().templates();
        templates.entries().forEach((id, value) -> result.add(encode(generation, TooltipDefinitionPayload.Kind.ENTRY, id, DescriptionDocument.CODEC, JsonOps.INSTANCE, () -> EquipmentDescriptions.entry(value,(kind,key)->EquipmentDescriptions.reference(definitions.domains(),definitions.behaviors(),kind,key)))));
        templates.affixes().forEach((id, value) -> result.add(encode(generation, TooltipDefinitionPayload.Kind.AFFIX, id, DescriptionDocument.CODEC, JsonOps.INSTANCE, () -> EquipmentDescriptions.affix(value,(kind,key)->EquipmentDescriptions.reference(definitions.domains(),definitions.behaviors(),kind,key),key->definitions.entry(key).map(e->EquipmentDescriptions.entry(e,(kind,ref)->EquipmentDescriptions.reference(definitions.domains(),definitions.behaviors(),kind,ref)))))));
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        // Item descriptions are independent of the player's active effects and their amplifiers.
        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.listElements().forEach(holder -> {
            var id = NativeIds.nexus(holder.key().identifier());
            if (!id.getNamespace().equals("minecraft")) return;
            var description = id.getPath().equals("resistance")
                ? io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.item_effect.resistance_per_level",
                    TooltipNumbers.percent(io.github.fatalitaya.combatnexus.formula.MultiplierFormula.resistanceEffectReduction(0)))
                : io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.native_effect.minecraft." + id.getPath());
            result.add(encode(generation, TooltipDefinitionPayload.Kind.ITEM_EFFECT, id, DescriptionDocument.CODEC, ops,
                () -> DescriptionDocument.leaf(new TooltipBlock(io.github.fatalitaya.combatnexus.presentation.platform.NativeDisplayTexts.fromComponent(holder.value().getDisplayName()), List.of(description), List.of()))));
        });
        definitions.domains().effects().forEach((id, value) -> result.add(encode(generation, TooltipDefinitionPayload.Kind.EFFECT, id, DescriptionDocument.CODEC, ops, () -> DescriptionDocument.leaf(EffectTooltip.describe(id, value)))));
        definitions.behaviors().forEach((id, value) -> result.add(encode(generation, TooltipDefinitionPayload.Kind.BEHAVIOR, id, DescriptionDocument.CODEC, ops,
                () -> DescriptionDocument.leaf(EffectTooltip.describe(id, new EffectDefinition(0, value.lifetime(), EffectService.Stacking.REPLACE, EffectPolicy.DEFAULT, List.of(), value.plan(), Set.of()))))));
        definitions.domains().shields().keySet().forEach(id->result.add(encode(generation,TooltipDefinitionPayload.Kind.SHIELD,id,DescriptionDocument.CODEC,ops,()->EquipmentDescriptions.reference(definitions.domains(),definitions.behaviors(),io.github.fatalitaya.combatnexus.combat.api.entry.EntryContribution.Kind.SHIELD,id).orElseThrow())));
        definitions.domains().regeneration().keySet().forEach(id->result.add(encode(generation,TooltipDefinitionPayload.Kind.REGENERATION,id,DescriptionDocument.CODEC,ops,()->EquipmentDescriptions.reference(definitions.domains(),definitions.behaviors(),io.github.fatalitaya.combatnexus.combat.api.entry.EntryContribution.Kind.REGENERATION,id).orElseThrow())));
        definitions.domains().abilities().forEach((id,value)->result.add(encode(generation,TooltipDefinitionPayload.Kind.ABILITY,id,DescriptionDocument.CODEC,ops,()->AbilityDescriptions.describe(value))));
        return List.copyOf(result);
    }
    private static <T> TooltipDefinitionPayload encode(long generation, TooltipDefinitionPayload.Kind kind, NexusId id, Codec<T> codec, DynamicOps<JsonElement> ops, java.util.function.Supplier<T> value) {
        // A definition too large to display remains explicitly unavailable; never truncate its conditions.
        String json;
        try { json = codec.encodeStart(ops, value.get()).getOrThrow().toString(); }
        catch (Exception failure) {
            json = "";
        }
        if (json.length() > TooltipDefinitionPayload.MAX_JSON) json = "";
        return new TooltipDefinitionPayload(generation, kind, id, json);
    }
}
