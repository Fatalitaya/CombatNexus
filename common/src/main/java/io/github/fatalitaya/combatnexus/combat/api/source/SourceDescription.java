package io.github.fatalitaya.combatnexus.combat.api.source;
import java.util.Objects;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
/** Display-only explanation of a captured source, never installation authority. */
public record SourceDescription(NexusId source, String slot, DisplayText description) {
    public SourceDescription { Objects.requireNonNull(source);Objects.requireNonNull(slot);Objects.requireNonNull(description); if(slot.length()>256)throw new IllegalArgumentException("Source slot too long"); }
}
