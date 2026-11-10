package io.github.fatalitaya.combatnexus.combat.regeneration.api;

import java.util.UUID;

/** Fractional game-tick recovery, no offline compensation or direct health writes. */
public interface RegenerationScheduler {
    void replace(RegenerationPlan plan);
    void remove(UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId source);
}
