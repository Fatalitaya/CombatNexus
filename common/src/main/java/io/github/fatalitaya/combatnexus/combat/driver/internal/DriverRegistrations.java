package io.github.fatalitaya.combatnexus.combat.driver.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.driver.api.*;
import java.util.*;

public final class DriverRegistrations {
    private DriverRegistrations() {}
    private record Credential(DriverDefinition definition) implements DriverRegistration { public NexusId id() { return definition.id(); } }
    private static final Map<NexusId, Credential> registered = new LinkedHashMap<>();
    private static boolean frozen;
    public static DriverRegistration register(DriverDefinition definition) {
        if (frozen) throw new IllegalStateException("Driver registration frozen");
        Objects.requireNonNull(definition);
        if (registered.size() >= 64) throw new IllegalArgumentException("Driver registration capacity");
        var value = new Credential(definition);
        if (registered.putIfAbsent(definition.id(), value) != null) throw new IllegalArgumentException("Duplicate driver: " + definition.id());
        return value;
    }
    public static void freeze() { frozen = true; }
    public static DriverDefinition require(DriverRegistration credential) {
        Objects.requireNonNull(credential);
        if (!frozen || registered.get(credential.id()) != credential) throw new IllegalArgumentException("Unregistered driver credential");
        return ((Credential) credential).definition();
    }
}
