package io.github.fatalitaya.combatnexus.presentation.api;

import java.util.List;
import java.util.Optional;

/** Detached explanation. The network representation is DescriptionDocument.CODEC. */
public record TooltipBlock(Optional<DisplayText> title, List<DisplayText> summary, List<DisplayText> details) {
    public TooltipBlock {
        java.util.Objects.requireNonNull(title);
        summary = List.copyOf(summary);
        details = List.copyOf(details);
    }
    public TooltipBlock(DisplayText title, List<DisplayText> summary, List<DisplayText> details) {
        this(Optional.of(title), summary, details);
    }
}
