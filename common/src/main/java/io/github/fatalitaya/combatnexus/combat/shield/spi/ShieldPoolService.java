package io.github.fatalitaya.combatnexus.combat.shield.spi;
import io.github.fatalitaya.combatnexus.combat.shield.api.*;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.UUID;

/** Server-owned pool lifecycle. All calls belong to one world session and its server thread. */
public interface ShieldPoolService extends ShieldService {
    ShieldGrantReceipt replace(UUID target, ShieldGrant pool);
    ShieldGrantReceipt replace(UUID target, ShieldGrant pool, io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent parent);
    Reservation reserve(UUID target, long damageId, NexusId damageType, double amount,
                        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent parent);
    void remove(UUID target, NexusId source, ShieldPoolSnapshot.RemovalReason reason);

    record Change(UUID target, ShieldPoolSnapshot pool, ShieldPoolSnapshot.RemovalReason reason) {}
}
