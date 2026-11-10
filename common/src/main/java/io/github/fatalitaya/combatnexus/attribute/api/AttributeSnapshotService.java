package io.github.fatalitaya.combatnexus.attribute.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.platform.AttributeObservationAccess;

/** Server-session revision allocator; captures authoritative values once for downstream consumers. */
public final class AttributeSnapshotService {
    private final Map<UUID, AttributeSnapshot> latest = new HashMap<>();
    private long revision;

    public AttributeSnapshot capture(UUID entity, AttributeObservationAccess access, Iterable<NexusId> requested) {
        return capture(entity, access, requested, count -> {});
    }
    public AttributeSnapshot capture(UUID entity, AttributeObservationAccess access, Iterable<NexusId> requested,
                                     java.util.function.IntConsumer budget) {
        long next = Math.incrementExact(revision);
        AttributeSnapshot snapshot = AttributeReader.capture(access.capture(requested, budget, latest.containsKey(entity) ? latest.get(entity).provenance().sources() : Map.of()), next);
        revision = next;
        latest.put(snapshot.valueFrame().entity(), snapshot);
        return snapshot;
    }

    public java.util.Optional<AttributeSnapshot> latest(UUID entity) {
        return java.util.Optional.ofNullable(latest.get(entity));
    }
    public void clear(UUID entity) { latest.remove(entity); }
    /** Revisions remain monotonic for the lifetime of this server service. */
    public void clear() { latest.clear(); }
}
