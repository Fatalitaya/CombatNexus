package io.github.fatalitaya.combatnexus.combat.damage.api.display.phrase;
import io.github.fatalitaya.combatnexus.presentation.api.phrase.*;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind;
import java.util.Objects;

public record RequestKindValue(DamageRequestKind kind) implements PhraseValue {
    public RequestKindValue {
        Objects.requireNonNull(kind, "kind");
    }
}
