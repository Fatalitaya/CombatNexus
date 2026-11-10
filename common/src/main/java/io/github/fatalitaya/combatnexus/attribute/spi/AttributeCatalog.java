package io.github.fatalitaya.combatnexus.attribute.spi;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;

/** Read-only registered attribute metadata, installed by the platform composition root. */
public final class AttributeCatalog {
    private AttributeCatalog() {}
    private static Port port;
    public static void install(Port implementation) {
        if (port != null) throw new IllegalStateException("AttributeCatalog already installed");
        port = Objects.requireNonNull(implementation);
    }
    public static Port get() { return Objects.requireNonNull(port, "AttributeCatalog not installed"); }
    public enum Vanilla { ATTACK_DAMAGE, ATTACK_SPEED, MOVEMENT_SPEED, ARMOR, ARMOR_TOUGHNESS, MAX_HEALTH, MAX_ABSORPTION }
    public interface Port {
        NexusId vanilla(Vanilla attribute);
        boolean exists(NexusId id);
        String descriptionId(NexusId id);
    }
}
