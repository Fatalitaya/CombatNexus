package io.github.fatalitaya.combatnexus.combat.time.api;

import java.util.Objects;
import java.util.Optional;

/** Read-only clock progress. The latest incomplete request can be resumed after driver rebind. */
public record TimeStatus(long tick, long nextSequence, Optional<TimeAdvanceReceipt> latest) {
    public TimeStatus {
        Objects.requireNonNull(latest);
        if(tick<0 || nextSequence<0)throw new IllegalArgumentException("Invalid time status");
    }
}
