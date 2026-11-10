package io.github.fatalitaya.combatnexus.combat.time.api;

import java.util.Objects;
import java.util.UUID;

/** Progress for one explicitly submitted increment, including a resumable unfinished slice. */
public record TimeAdvanceReceipt(long sequence, UUID request, long requested, long consumed, long remaining, boolean completed, boolean cancelled) {
    public TimeAdvanceReceipt {
        Objects.requireNonNull(request);
        if (sequence < 0 || requested < 0 || consumed < 0 || remaining < 0 || consumed + remaining != requested
                || completed != (remaining == 0) || cancelled && completed) throw new IllegalArgumentException("Invalid time progress");
    }
    public boolean terminal() { return completed || cancelled; }
}
