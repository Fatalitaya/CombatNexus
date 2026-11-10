package io.github.fatalitaya.combatnexus.combat.time.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.UUID;

/** Read-only business time. Reading does not advance a clock or install a source. */
@FunctionalInterface
public interface BusinessTime {
    long now(UUID target, NexusId source);
    /** Phase of an opaque native periodic effect, preserving its original entity-age offset. */
    default long phase(UUID target, NexusId source, long nativePhase) { return nativePhase; }
    /** Whether this lifecycle slice advances the effective clock of this source. */
    default boolean advancing(UUID target, NexusId source) { return true; }
    /** Budget check before consuming a due event; a refusal leaves it pending. */
    default boolean mayExecute() { return true; }
}
