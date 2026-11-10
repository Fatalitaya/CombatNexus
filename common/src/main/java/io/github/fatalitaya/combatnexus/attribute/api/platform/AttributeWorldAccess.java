package io.github.fatalitaya.combatnexus.attribute.api.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import java.util.Optional;
import java.util.UUID;

/** World-owned native writes used exclusively by the managed contribution transaction. */
public interface AttributeWorldAccess {
    /** Requires the world thread and returns the active session, or throws if closed. */
    UUID requireSession();
    Optional<UUID> session();
    void requireLivingTarget(UUID target);
    Slot attribute(UUID target, NexusId attribute);

    /** Stable identity of one loaded entity's attribute, including across repeated lookups. */
    interface Slot {
        Optional<AttributeSnapshot.Contribution> find(NexusId id);
        void install(AttributeSnapshot.Contribution modifier);
        void remove(NexusId id);
    }
}
