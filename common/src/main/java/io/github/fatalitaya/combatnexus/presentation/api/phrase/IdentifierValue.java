package io.github.fatalitaya.combatnexus.presentation.api.phrase;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;

public record IdentifierValue(NexusId identifier) implements PhraseValue {
    public IdentifierValue {
        Objects.requireNonNull(identifier, "identifier");
    }
}
