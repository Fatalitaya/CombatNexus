package io.github.fatalitaya.combatnexus.presentation.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
import java.util.function.BiFunction;

/** Shared carried-effect description composition; never executes native effects. */
public final class ItemPotionTooltipProjector {
    private ItemPotionTooltipProjector() {}
    public static List<TooltipBlock> project(List<CarriedEffectObservation> observed,
            BiFunction<NexusId, Integer, Optional<TooltipBlock>> descriptions) {
        var result = new ArrayList<TooltipBlock>();
        var budget = new PresentationBudget(4096, 24000);
        for (var effect : observed) {
            budget.entry(1);
            var summary = new ArrayList<DisplayText>();
            if (effect.probability() < 1) summary.add(GeneratedTexts.translatable("tooltip.combatnexus.item_effect.probability", TooltipNumbers.percent(effect.probability())));
            var adopted = descriptions.apply(effect.effect(), effect.amplifier()).orElseThrow(() -> new IllegalArgumentException("Missing carried effect description"));
            summary.addAll(adopted.summary());
            for (var attribute : effect.attributes()) summary.add(AttributeDescription.describe(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().descriptionId(attribute.attribute())), attribute.attribute(), attribute.modifier()));
            result.add(new TooltipBlock(effect.title(), summary, adopted.details()));
        }
        return List.copyOf(result);
    }
}
