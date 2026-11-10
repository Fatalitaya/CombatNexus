package io.github.fatalitaya.combatnexus.presentation.api.phrase;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;

public record RulePhraseType(NexusId id) {
    public RulePhraseType {
        Objects.requireNonNull(id, "id");
    }

    public static RulePhraseType of(String namespace, String path) {
        return new RulePhraseType(
                NexusId.fromNamespaceAndPath(namespace, path)
        );
    }

    public String translationKey(
            PhraseVariant variant,
            PhraseForm form
    ) {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(form, "form");
        return "rule_phrase."
                + id.getNamespace()
                + "."
                + id.getPath().replace('/', '.')
                + "."
                + variant.serializedName()
                + "."
                + form.serializedName();
    }
}
