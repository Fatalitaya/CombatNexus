package io.github.fatalitaya.combatnexus.combat.shield.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal;

/** Confirmed pool writes, available before any observers are scheduled. */
public record ShieldCommitReceipt(UUID target, long applicationId, double absorbed,
                                  List<ApplicationJournal.Entry> writes, Map<NexusId, Double> absorbedChannels) {
    public ShieldCommitReceipt {
        Objects.requireNonNull(target); writes = List.copyOf(writes);
        absorbedChannels = Map.copyOf(absorbedChannels);
        io.github.fatalitaya.combatnexus.formula.Amounts.nonNegative(absorbed, "absorbed");
        if (applicationId < 1) throw new IllegalArgumentException("Invalid application identity");
    }
}
