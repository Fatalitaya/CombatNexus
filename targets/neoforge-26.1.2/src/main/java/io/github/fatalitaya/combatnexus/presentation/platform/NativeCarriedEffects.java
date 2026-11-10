package io.github.fatalitaya.combatnexus.presentation.platform;

import io.github.fatalitaya.combatnexus.presentation.api.*;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeCalculation;
import io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import java.util.*;

public final class NativeCarriedEffects {
    private NativeCarriedEffects() {}
    public static List<CarriedEffectObservation> capture(ItemStack stack, float tickRate, boolean advanced) {
        var display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (display.hideTooltip()) return List.of();
        var result = new ArrayList<CarriedEffectObservation>();
        var budget = new PresentationBudget(4096, 24000);
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && display.shows(DataComponents.POTION_CONTENTS)) {
            float scale = stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0f);
            for (var effect : potion.getAllEffects())
                result.add(describe(effect, scale, tickRate, 1, false, budget));
        }
        var consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null && display.shows(DataComponents.CONSUMABLE)) {
            for (var action : consumable.onConsumeEffects()) if (action instanceof ApplyStatusEffectsConsumeEffect apply) {
                for (var effect : apply.effects())
                    result.add(describe(effect, 1, tickRate, apply.probability(), true, budget));
            }
        }
        // Preserve vanilla's deliberate concealment of suspicious stew in normal tooltips.
        var stew = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (advanced && stew != null && display.shows(DataComponents.SUSPICIOUS_STEW_EFFECTS)) {
            for (var entry : stew.effects())
                result.add(describe(entry.createEffectInstance(), 1, tickRate, 1, false, budget));
        }
        return List.copyOf(result);
    }
    private static CarriedEffectObservation describe(MobEffectInstance instance, float scale, float tickRate,
            float probability, boolean attributes, PresentationBudget budget) {
        budget.entry(1);
        var lines = new ArrayList<Component>();
        PotionContents.addPotionTooltip(List.of(instance), lines::add, scale, tickRate);
        var parameters = new ArrayList<CarriedEffectObservation.AttributeParameter>();
        if (attributes) instance.getEffect().value().createModifiers(instance.getAmplifier(), (attribute, modifier) ->
            parameters.add(new CarriedEffectObservation.AttributeParameter(AttributeAccess.get().attributeId(attribute.value()),
                    new AttributeCalculation.Modifier(AttributeAccess.get().modifierId(modifier), modifier.amount(), AttributeAccess.get().operation(modifier)))));
        return new CarriedEffectObservation(NativeIds.nexus(instance.getEffect().unwrapKey().orElseThrow().identifier()), instance.getAmplifier(),
                io.github.fatalitaya.combatnexus.presentation.platform.NativeDisplayTexts.fromComponent(lines.getFirst()), probability, parameters);
    }
}
