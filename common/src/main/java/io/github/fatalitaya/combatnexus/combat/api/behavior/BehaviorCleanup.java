package io.github.fatalitaya.combatnexus.combat.api.behavior;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** Revocation-only capability; it cannot grant resources or submit combat requests. */
public interface BehaviorCleanup { void removeShield(NexusId pool); }
