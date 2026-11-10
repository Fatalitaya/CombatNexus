package io.github.fatalitaya.combatnexus.combat.api.transaction;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;
import java.util.UUID;

/** Identity of one loaded world lifetime, distinct even when the dimension is reloaded. */
public final class WorldSession {
    private final ServerSession server;
    private final NexusId dimension;
    private final UUID id = UUID.randomUUID();
    public WorldSession(ServerSession server, NexusId dimension) {
        this.server = Objects.requireNonNull(server);
        this.dimension = Objects.requireNonNull(dimension);
    }
    public ServerSession server() { return server; }
    public NexusId dimension() { return dimension; }
    public UUID id() { return id; }
}
