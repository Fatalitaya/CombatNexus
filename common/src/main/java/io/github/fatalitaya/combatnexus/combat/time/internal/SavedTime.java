package io.github.fatalitaya.combatnexus.combat.time.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Business clock values and ownership. No live driver sessions survive storage. */
public record SavedTime(long entityTick, long phaseOffset, NexusId entityDriver, List<Source> sources, List<Advance> advances) {
    public record Source(NexusId source, long tick, NexusId driver) {
        public Source { Objects.requireNonNull(source); if (tick < 0) throw new IllegalArgumentException("Negative source time"); }
    }
    public record Advance(long sequence, UUID request, NexusId source, NexusId driver, long requested, long consumed, boolean slicePending, boolean cancelled) {
        public Advance {
            Objects.requireNonNull(request); Objects.requireNonNull(driver);
            if (sequence < 0 || requested < 1 || requested > 1200000 || consumed < 0 || consumed > requested || slicePending && consumed == requested)
                throw new IllegalArgumentException("Invalid saved advancement");
        }
    }
    public SavedTime {
        sources = List.copyOf(sources); advances = List.copyOf(advances);
        if (entityTick < 0 || sources.size() > 256 || advances.size() > 257) throw new IllegalArgumentException("Time state capacity");
        if (sources.stream().map(Source::source).distinct().count() != sources.size()
                || advances.stream().map(Advance::source).distinct().count() != advances.size())
            throw new IllegalArgumentException("Duplicate time identity");
    }
}
