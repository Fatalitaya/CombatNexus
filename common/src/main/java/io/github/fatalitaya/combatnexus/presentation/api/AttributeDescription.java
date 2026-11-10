package io.github.fatalitaya.combatnexus.presentation.api;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeUnits;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Set;

/** Shared attribute units for parameter descriptions; never computes effective values. */
public final class AttributeDescription {
    private AttributeDescription() {}
    /** Formats a definition parameter, not a recomputed effective attribute value. */
    public static DisplayText describe(DisplayText name, NexusId attributeId, io.github.fatalitaya.combatnexus.attribute.api.AttributeCalculation.Modifier modifier) {
        var unit = AttributeUnits.unit(attributeId).map(Enum::name).orElse("");
        String amount = TooltipNumbers.number(modifier.amount());
        String operation;
        switch (modifier.operation()) {
            case ADD_VALUE -> {
                boolean ratio = Set.of("PROBABILITY", "ADDITIVE_RATIO", "CONVERSION_RATIO").contains(unit);
                operation = ratio ? "attribute.points" : "attribute.add";
                if (ratio) amount = TooltipNumbers.number(modifier.amount() * 100);
            }
            case ADD_MULTIPLIED_BASE -> { operation = "attribute.base"; amount = TooltipNumbers.percent(modifier.amount()); }
            case ADD_MULTIPLIED_TOTAL -> { operation = "attribute.total"; amount = TooltipNumbers.percent(modifier.amount()); }
            default -> throw new IllegalStateException("Unknown attribute operation");
        }
        return text(operation, name, amount);
    }
    private static DisplayText text(String key, Object... args) { return GeneratedTexts.translatable("tooltip.combatnexus.effect." + key, args); }
}
