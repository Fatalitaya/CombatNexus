package io.github.fatalitaya.combatnexus.combat.shield.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
/** Business grants and queries. Reservation and application commit belong exclusively to the application SPI. */
public interface ShieldCommands {
    List<ShieldPoolSnapshot> snapshot(UUID target);
    CommandResult<ShieldGrantReceipt> grant(UUID request, UUID target, ShieldGrant grant);
    CommandResult<ShieldGrantReceipt> grant(UUID request, UUID target, ShieldGrant grant, CombatParent parent);
    CommandResult<Boolean> remove(UUID request, UUID target, NexusId source);
}
