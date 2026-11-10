package io.github.fatalitaya.combatnexus.combat.damage.api.display.document;

import io.github.fatalitaya.combatnexus.combat.damage.api.display.narrative.RuleNarrative;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;
import java.util.Objects;

public record VanillaTooltipAugmentation(
        NexusId enchantmentId,
        DisplayText originalLineAnchor,
        RuleNarrative narrative,
        List<VanillaTooltipNote> detailNotes,
        List<String> applicationBuckets,
        List<NexusId> conditionTypeIds,
        List<NexusId> operationTypeIds
) implements TooltipSection {
    public VanillaTooltipAugmentation {
        Objects.requireNonNull(enchantmentId, "enchantmentId");
        Objects.requireNonNull(originalLineAnchor, "originalLineAnchor");
        Objects.requireNonNull(narrative, "narrative");
        detailNotes = detailNotes == null ? List.of() : List.copyOf(detailNotes);
        applicationBuckets = applicationBuckets == null ? List.of() : List.copyOf(applicationBuckets);
        conditionTypeIds = conditionTypeIds == null ? List.of() : List.copyOf(conditionTypeIds);
        operationTypeIds = operationTypeIds == null ? List.of() : List.copyOf(operationTypeIds);
    }
}
