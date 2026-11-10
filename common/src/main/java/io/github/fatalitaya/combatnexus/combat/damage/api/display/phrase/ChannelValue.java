package io.github.fatalitaya.combatnexus.combat.damage.api.display.phrase;
import io.github.fatalitaya.combatnexus.presentation.api.phrase.*;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;

public record ChannelValue(NexusId channelId) implements PhraseValue {
    public ChannelValue {
        Objects.requireNonNull(channelId, "channelId");
    }
}
