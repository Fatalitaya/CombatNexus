package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit;
import io.github.fatalitaya.combatnexus.presentation.api.TooltipBlock;
import io.github.fatalitaya.combatnexus.presentation.api.TooltipNumbers;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Shared definition explanation for equipment and effect tooltips, without live entity queries. */
public final class EffectTooltip {
    private EffectTooltip() {}
    /** Registered platform description at the carried amplifier, without a live entity or settlement. */
    public static List<DisplayText> capability(NexusId id, int amplifier) {
        return List.copyOf(io.github.fatalitaya.combatnexus.effect.internal.EffectCapabilityDescriptions.describeRequired(id, amplifier));
    }
    public static TooltipBlock describe(NexusId id, EffectDefinition definition) {
        return describe(id, definition, definition.level(), Optional.empty(), Optional.empty(), true, definition.level());
    }
    private static TooltipBlock describe(NexusId id, EffectDefinition definition, int level,
            Optional<EffectView> instance, Optional<DisplayText> source, boolean lifetime, int displayedLevel) {
        var summary = new ArrayList<DisplayText>();
        var details = new ArrayList<DisplayText>();
        for (var attribute : definition.attributes()) {
            var contribution = attribute.modifier();
            summary.add(io.github.fatalitaya.combatnexus.presentation.api.AttributeDescription.describe(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().descriptionId(attribute.attribute())), attribute.attribute(), new io.github.fatalitaya.combatnexus.attribute.api.AttributeCalculation.Modifier(contribution.source(), contribution.amount(), contribution.operation())));
        }
        definition.restrictions().stream().sorted().forEach(action -> summary.add(text("restriction." + action.name().toLowerCase(Locale.ROOT))));
        for (var behavior : definition.behavior().components()) {
            var explanation = io.github.fatalitaya.combatnexus.effect.internal.EffectBehaviorDescriptors.describe(behavior, level);
            summary.addAll(explanation.summary());
            details.addAll(explanation.details());
            details.addAll(io.github.fatalitaya.combatnexus.effect.internal.EffectBehaviorDescriptors.periodicDetails(behavior));
        }
        if (lifetime) summary.add(instance.map(view -> view.remainingTicks().isPresent()
            ? text("remaining", TooltipNumbers.number(Math.max(0, view.remainingTicks().getAsLong()) / 20.0))
            : text("permanent")).orElseGet(() -> switch (definition.lifetime().kind()) {
            case TIMED -> text("duration", TooltipNumbers.number(definition.lifetime().ticks() / 20.0));
            case INSTANT -> text("instant");
            case PERMANENT -> text("permanent");
        }));
        details.add(0, text("level", displayedLevel));
        source.ifPresent(value -> details.add(text("source", value)));
        details.add(text("stacking." + definition.stacking().getPath()));
        details.add(text(definition.policy().dispellable() ? "dispellable" : "not_dispellable"));
        return new TooltipBlock(GeneratedTexts.translatableWithFallback("effect." + id.getNamespace() + "." + id.getPath().replace('/', '.'), id.getPath().replace('_', ' ')), summary, details);
    }
    public static TooltipBlock instance(NexusId id, EffectDefinition definition, EffectView view, DisplayText source) {
        return describe(id, definition, view.level(), Optional.of(view), Optional.of(source), true, view.level());
    }

    public static TooltipBlock nativeInstance(NexusId id, EffectDefinition definition) {
        return describe(id, definition, definition.level(), Optional.empty(), Optional.empty(), false, definition.level() + 1);
    }
    private static DisplayText limit(TriggerLimit limit) {
        return text("limit", text("scope." + limit.scope().name().toLowerCase(Locale.ROOT)), limit.maximum());
    }
    private static DisplayText text(String key, Object... args) { return GeneratedTexts.translatable("tooltip.combatnexus.effect." + key, args); }
}
