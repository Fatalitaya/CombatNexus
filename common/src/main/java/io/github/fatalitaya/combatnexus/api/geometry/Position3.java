package io.github.fatalitaya.combatnexus.api.geometry;

/** Detached world-space position; conversion to a native vector belongs to the target. */
public record Position3(double x, double y, double z) {
    public Position3 {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Position must be finite");
    }
}
