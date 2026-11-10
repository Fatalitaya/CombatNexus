package io.github.fatalitaya.combatnexus.combat.api.transaction;

/** Identity of one server lifetime. Contains no platform object and grants no authority. */
public final class ServerSession {
    private final java.util.UUID id = java.util.UUID.randomUUID();
    public java.util.UUID id() { return id; }
    public ServerSession() {}
}
