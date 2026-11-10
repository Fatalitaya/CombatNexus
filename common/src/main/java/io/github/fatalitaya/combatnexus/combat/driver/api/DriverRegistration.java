package io.github.fatalitaya.combatnexus.combat.driver.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Opaque startup credential. A matching descriptive ID does not authorize a session. */
public interface DriverRegistration { NexusId id(); }
