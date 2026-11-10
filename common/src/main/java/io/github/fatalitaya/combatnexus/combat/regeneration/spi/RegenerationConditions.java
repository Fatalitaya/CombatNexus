package io.github.fatalitaya.combatnexus.combat.regeneration.spi;

import java.util.OptionalLong;
import java.util.Objects;
import java.util.UUID;

/** Captured by the server adapter; damage time uses confirmed loss, never Incoming proposals. */
@FunctionalInterface
public interface RegenerationConditions {
    State read(UUID target);

    record State(boolean alive, boolean naturalRegeneration) {}
}
