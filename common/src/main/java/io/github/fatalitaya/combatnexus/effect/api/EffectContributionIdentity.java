package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Source ownership decides the stable identity; attribute only installs the supplied contribution. */
public final class EffectContributionIdentity {
    private EffectContributionIdentity() {}
    public static NexusId id(EffectInstance effect, EffectInstance.AttributeContribution contribution) {
        String identity = effect.snapshot().effect() + "|" + effect.snapshot().source()
                + "|" + contribution.attribute() + "|" + contribution.modifier().source();
        return NexusId.fromNamespaceAndPath("combatnexus", "effect/"
                + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)));
    }
}
