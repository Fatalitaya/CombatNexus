package io.github.fatalitaya.combatnexus.combat.api.platform;

import java.util.UUID;

/** World-local state cleanup at explicit domain boundaries. */
public interface ParticipantStateAccess {
    void actionFinished(UUID participant, UUID action);
    void sourceRemoved(UUID participant, UUID source);
}
