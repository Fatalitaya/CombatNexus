package io.github.fatalitaya.combatnexus.combat.platform;

import java.util.concurrent.atomic.AtomicLong;

public final class ParticipantIncarnations {
    private static final AtomicLong NEXT = new AtomicLong();
    private ParticipantIncarnations() {}
    public static long next() {
        long value = NEXT.incrementAndGet();
        if (value <= 0) throw new IllegalStateException("Participant incarnations exhausted");
        return value;
    }
}
