package io.github.fatalitaya.combatnexus.attribute.api.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeObservation;
import java.util.function.IntConsumer;

/** Request-bound attribute observation for the exact participant; no policy is evaluated during downstream calculation. */
@FunctionalInterface
public interface AttributeObservationAccess {
    AttributeObservation capture(Iterable<NexusId> requested, IntConsumer budget, java.util.Map<NexusId, java.util.List<io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot.Contribution>> previous);
}
