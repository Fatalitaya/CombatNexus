package io.github.fatalitaya.combatnexus.combat.damage.util;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

public final class IdentifierText {
    private IdentifierText() {
    }

    public static String namespace(NexusId id) {
        String raw = id.toString();
        int colon = raw.indexOf(':');
        return colon >= 0 ? raw.substring(0, colon) : "minecraft";
    }

    public static String path(NexusId id) {
        String raw = id.toString();
        int colon = raw.indexOf(':');
        return colon >= 0 && colon + 1 < raw.length()
                ? raw.substring(colon + 1)
                : raw;
    }

    public static String langPath(NexusId id) {
        return namespace(id) + "." + path(id);
    }

}
