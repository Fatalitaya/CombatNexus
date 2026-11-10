package io.github.fatalitaya.combatnexus.combat.healing.api;

import java.util.Objects;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Captured calculation evidence, not an application receipt. Additive intermediates may be negative until clamped. */
public record HealingContribution(NexusId source, Kind kind, double before, double after) {
    public static final int MAX_FACTS = 256; // 64 sources + 64 bounded rules (additive/multiplier) + phase clamps
    public enum Kind { ADDITIVE, CLAMP, SOURCE, RECEIVED, ADJUSTMENT }
    public HealingContribution {
        Objects.requireNonNull(source); Objects.requireNonNull(kind);
        if (source.toString().length() > 256 || !Double.isFinite(before) || !Double.isFinite(after))
            throw new IllegalArgumentException("Invalid healing contribution fact");
    }
}
