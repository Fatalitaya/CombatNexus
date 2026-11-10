package io.github.fatalitaya.combatnexus.combat.damage.api.display.phrase;
import io.github.fatalitaya.combatnexus.presentation.api.phrase.*;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;

public record TagValue(Kind kind, NexusId tagId) implements PhraseValue {
    public TagValue {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tagId, "tagId");
    }

    public enum Kind {
        MOB_EFFECT("mob_effect"),
        ENTITY_TYPE("entity_type"),
        DAMAGE_TYPE("damage_type"),
        SOURCE("source");

        private final String serializedName;

        Kind(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }
    }
}
