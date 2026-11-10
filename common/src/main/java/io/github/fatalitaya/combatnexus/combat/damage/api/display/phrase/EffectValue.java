package io.github.fatalitaya.combatnexus.combat.damage.api.display.phrase;
import io.github.fatalitaya.combatnexus.presentation.api.phrase.*;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;

public record EffectValue(NexusId effectId) implements PhraseValue {
    public EffectValue {
        Objects.requireNonNull(effectId, "effectId");
    }
}
