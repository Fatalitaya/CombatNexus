package io.github.fatalitaya.combatnexus.combat.damage.api.display.phrase;
import io.github.fatalitaya.combatnexus.presentation.api.phrase.*;

import io.github.fatalitaya.combatnexus.api.entity.EntityCategory;
import java.util.Objects;

public record MobCategoryValue(EntityCategory category) implements PhraseValue {
    public MobCategoryValue {
        Objects.requireNonNull(category, "category");
    }
}
