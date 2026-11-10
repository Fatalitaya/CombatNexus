package io.github.fatalitaya.combatnexus.combat.api.source;

import java.util.Objects;
import java.util.UUID;

/** Detached current-format definition values; restore validates registered executors and native resources again. */
public record DefinitionBindingSnapshot(UUID semanticIdentity, String content) {
    public DefinitionBindingSnapshot {
        Objects.requireNonNull(semanticIdentity); Objects.requireNonNull(content);
        if (content.isBlank() || content.length() > 1048576) throw new IllegalArgumentException("Definition binding budget");
    }
}
