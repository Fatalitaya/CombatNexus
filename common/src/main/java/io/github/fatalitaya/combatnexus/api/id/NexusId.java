package io.github.fatalitaya.combatnexus.api.id;

import java.util.Optional;

/** Business identity; text and ordering follow the 26.1.2 baseline. */
public final class NexusId implements Comparable<NexusId> {
    private final String namespace;
    private final String path;
    public NexusId(String namespace, String path) {
        if (namespace == null || path == null) throw new IllegalArgumentException("ID parts must not be null");
        if (namespace.equals("..") || !valid(namespace, false) || !valid(path, true))
            throw new IllegalArgumentException("Invalid ID: " + namespace + ":" + path);
        this.namespace = namespace;
        this.path = path;
    }
    private static boolean valid(String value, boolean path) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || path && c == '/')) return false;
        }
        return true;
    }
    public static NexusId fromNamespaceAndPath(String namespace, String path) { return new NexusId(namespace, path); }
    public static NexusId withDefaultNamespace(String path) { return new NexusId("minecraft", path); }
    public static NexusId parse(String text) {
        if (text == null) throw new IllegalArgumentException("ID must not be null");
        int colon = text.indexOf(':');
        return colon < 0 ? withDefaultNamespace(text) : new NexusId(colon == 0 ? "minecraft" : text.substring(0, colon), text.substring(colon + 1));
    }
    public static Optional<NexusId> tryParse(String text) {
        try { return Optional.of(parse(text)); } catch (IllegalArgumentException invalid) { return Optional.empty(); }
    }
    public String namespace() { return namespace; }
    public String path() { return path; }
    @Override public boolean equals(Object other) {
        return other instanceof NexusId id && namespace.equals(id.namespace) && path.equals(id.path);
    }
    @Override public int hashCode() { return 31 * namespace.hashCode() + path.hashCode(); }
    public String getNamespace() { return namespace; }
    public String getPath() { return path; }
    @Override public String toString() { return namespace + ":" + path; }
    public int compareNamespaced(NexusId other) {
        int result = namespace.compareTo(other.namespace);
        return result == 0 ? path.compareTo(other.path) : result;
    }
    @Override public int compareTo(NexusId other) {
        int result = path.compareTo(other.path);
        return result == 0 ? namespace.compareTo(other.namespace) : result;
    }
}
