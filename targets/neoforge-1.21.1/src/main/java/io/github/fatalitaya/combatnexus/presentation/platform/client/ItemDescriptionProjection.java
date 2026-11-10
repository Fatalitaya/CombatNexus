package io.github.fatalitaya.combatnexus.presentation.platform.client;

import io.github.fatalitaya.combatnexus.presentation.client.ClientTooltipDefinitions;

import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import io.github.fatalitaya.combatnexus.combat.damage.api.item.*;
import io.github.fatalitaya.combatnexus.combat.damage.client.ClientFormulaSettings;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.presentation.api.*;
import io.github.fatalitaya.combatnexus.presentation.platform.NativeItemEffectObservation;
import io.github.fatalitaya.combatnexus.presentation.platform.NativeCarriedEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import java.util.*;

public final class ItemDescriptionProjection {
    private ItemDescriptionProjection() {}
    public static JsonObject capture(Player player, ItemStack stack) {
        stack = stack.copy();
        if (!io.github.fatalitaya.combatnexus.presentation.platform.NativeTooltipVisibility.shows(stack, DataComponents.ENCHANTMENTS)) stack.remove(DataComponents.ENCHANTMENTS);
        if (!io.github.fatalitaya.combatnexus.presentation.platform.NativeTooltipVisibility.shows(stack, io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageComponents.bindings())) stack.remove(io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageComponents.bindings());
        var result = new JsonObject();var documents=new JsonArray();result.add("documents",documents);
        for(var binding:ClientItemBindings.visible(stack).bindings())documents.add(DescriptionDocument.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
            EquipmentDescriptions.binding(binding,(kind,id)->ClientTooltipDefinitions.effect(id,ClientTooltipDefinitions.Kind.valueOf(kind.name())),ClientTooltipDefinitions::entry,ClientTooltipDefinitions::affix)).getOrThrow());
        var settings = ClientFormulaSettings.current();
        final var describedStack = stack;
        var equipmentBudget = new PresentationBudget(4096, 24000);
        var equipment = ItemEffectTooltipProjector.project(NativeItemEffectObservation.capture(player.registryAccess(), stack, equipmentBudget), (id, kind) ->
            Optional.of(block(ClientTooltipDefinitions.entry(id)
                .orElseThrow(() -> new IllegalArgumentException("Missing equipment description: " + id)))),
            settings.map(s -> s.managedEnchantments()).orElse(false),
            settings.map(s -> (double) s.formulas().ratingPerProtScore()).orElse(0.0), NativeItemEffectObservation.VALUES,
            () -> io.github.fatalitaya.combatnexus.presentation.platform.NativeItemComponentDescriptions.project(describedStack), equipmentBudget);
        var carriedStack = stack.copy(); carriedStack.remove(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        var stewStack = stack.copy(); stewStack.remove(DataComponents.POTION_CONTENTS); stewStack.remove(DataComponents.FOOD);
        java.util.function.BiFunction<io.github.fatalitaya.combatnexus.api.id.NexusId, Integer, Optional<TooltipBlock>> nativeDescription = (id, amplifier) -> {
            var holder = ContentAccess.get().effect(id);
            if (holder.isEmpty() || !id.getNamespace().equals("minecraft"))
                throw new IllegalArgumentException("Missing carried effect description: " + id);
            List<DisplayText> description = id.getPath().equals("resistance")
                ? List.of(GeneratedTexts.translatable("tooltip.combatnexus.item_effect.resistance_per_level", TooltipNumbers.percent(io.github.fatalitaya.combatnexus.formula.MultiplierFormula.resistanceEffectReduction(0))))
                : io.github.fatalitaya.combatnexus.effect.api.EffectTooltip.capability(id, amplifier);
            return Optional.of(new TooltipBlock(io.github.fatalitaya.combatnexus.presentation.platform.NativeDisplayTexts.fromComponent(holder.get().value().getDisplayName()), description, List.of()));
        };
        var budget = new PresentationBudget(4096, 24000);
        var ops = new BudgetJsonOps(budget);
        result.add("equipment", encode(equipment, ops));
        result.add("carried", encode(ItemPotionTooltipProjector.project(NativeCarriedEffects.capture(carriedStack, player.level().tickRateManager().tickrate(), false), nativeDescription), ops));
        result.add("stew", encode(ItemPotionTooltipProjector.project(NativeCarriedEffects.capture(stewStack, player.level().tickRateManager().tickrate(), true), nativeDescription), ops));
        return result;
    }
    private static void appendTemplate(JsonArray documents, Optional<DescriptionDocument> document) {
        documents.add(DescriptionDocument.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
            document.orElseThrow(() -> new IllegalArgumentException("Missing item description template"))).getOrThrow());
    }
    private static TooltipBlock block(DescriptionDocument document) {
        return new TooltipBlock(document.title(),
            (document.authoredSummary().isEmpty() ? document.generated() : document.authoredSummary()),
            document.details());
    }
    private static JsonArray encode(List<TooltipBlock> blocks, BudgetJsonOps ops) {
        var result = new JsonArray();
        for (var block : blocks) result.add(DescriptionDocument.CODEC.encodeStart(ops, DescriptionDocument.leaf(block)).getOrThrow());
        return result;
    }
}
