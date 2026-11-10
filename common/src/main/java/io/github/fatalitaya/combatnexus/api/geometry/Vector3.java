package io.github.fatalitaya.combatnexus.api.geometry;

/** Detached vector; callers decide whether non-finite observations are acceptable. */
public record Vector3(double x, double y, double z) {
    public boolean finite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    public Vector3 add(Vector3 other) { return new Vector3(x + other.x, y + other.y, z + other.z); }
}
