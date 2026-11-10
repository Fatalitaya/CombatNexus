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
import java.util.*;

public final class NativeCarriedEffects {
    private NativeCarriedEffects() {}
    public static List<CarriedEffectObservation> capture(ItemStack stack, float tickRate, boolean advanced) {
        if (stack.has(net.minecraft.core.component.DataComponents.HIDE_TOOLTIP)) return List.of();
        var result = new ArrayList<CarriedEffectObservation>();
        var budget = new PresentationBudget(4096, 24000);
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && io.github.fatalitaya.combatnexus.presentation.platform.NativeTooltipVisibility.shows(stack, DataComponents.POTION_CONTENTS)) {
            float scale = 1.0f;
            for (var effect : potion.getAllEffects())
                result.add(describe(effect, scale, tickRate, 1, false, budget));
        }
        var consumable = stack.get(DataComponents.FOOD);
        if (consumable != null && io.github.fatalitaya.combatnexus.presentation.platform.NativeTooltipVisibility.shows(stack, DataComponents.FOOD)) {
            for (var effect : consumable.effects()) result.add(describe(effect.effect(), 1, tickRate, effect.probability(), true, budget));
        }
        // Preserve vanilla's deliberate concealment of suspicious stew in normal tooltips.
        var stew = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (advanced && stew != null && io.github.fatalitaya.combatnexus.presentation.platform.NativeTooltipVisibility.shows(stack, DataComponents.SUSPICIOUS_STEW_EFFECTS)) {
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
        return new CarriedEffectObservation(NativeIds.nexus(instance.getEffect().unwrapKey().orElseThrow().location()), instance.getAmplifier(),
                io.github.fatalitaya.combatnexus.presentation.platform.NativeDisplayTexts.fromComponent(lines.getFirst()), probability, parameters);
    }
}
