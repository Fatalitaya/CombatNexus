package io.github.fatalitaya.combatnexus.combat.shield.spi;
import io.github.fatalitaya.combatnexus.combat.shield.api.*;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.List;
import java.util.UUID;

/** Server-owned total-damage shield contract. No channel precision is implied. */
public interface ShieldService {
    List<ShieldPoolSnapshot> snapshot(UUID target);
    List<ShieldPoolSnapshot> snapshot(UUID target, java.util.function.IntConsumer budget);

    /** Reserve after rejection/block checks; cancellation must release the reservation. */
    Reservation reserve(UUID target, long damageId, NexusId damageType, double amount);

    Reservation reserve(UUID target, long damageId, NexusId damageType, double amount,
            java.util.Map<NexusId, Float> channels);

    interface Reservation extends AutoCloseable {
        double absorbed();
        double remainingDamage();
        /** Confirm once at the actual application point, including full shield absorption. */
        ShieldCommitReceipt commit();
        java.util.Optional<ShieldCommitReceipt> receipt();
        /** Releases uncommitted reservations; closing a committed reservation does not undo it. */
        @Override void close();
    }
}
