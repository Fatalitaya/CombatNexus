package io.github.fatalitaya.combatnexus.attribute.platform.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Startup-only native attribute observation policies. Expires after event dispatch. */
public interface AttributeCaptureRegistrar {
    void register(NexusId id, int priority, EffectiveAttributePolicy policy);
}
