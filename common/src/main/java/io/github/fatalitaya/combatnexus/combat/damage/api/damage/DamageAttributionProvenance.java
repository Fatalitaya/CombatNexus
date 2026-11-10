package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;
import java.util.Optional;

/** Immutable framework-authored proof describing an attribution's source. */
public record DamageAttributionProvenance(
        DamageAttributionSource source,
        Optional<NexusId> resolverId
) {
    public DamageAttributionProvenance {
        source = Objects.requireNonNull(source, "source");
        resolverId = resolverId == null ? Optional.empty() : resolverId;
        if ((source == DamageAttributionSource.REGISTERED_RESOLVER)
                != resolverId.isPresent()) {
            throw new IllegalArgumentException(
                    "Only registered-resolver attribution has a resolver ID"
            );
        }
    }

    public static DamageAttributionProvenance vanillaDefault() {
        return new DamageAttributionProvenance(
                DamageAttributionSource.VANILLA_DEFAULT,
                Optional.empty()
        );
    }

    public static DamageAttributionProvenance publicRequest() {
        return new DamageAttributionProvenance(
                DamageAttributionSource.PUBLIC_REQUEST,
                Optional.empty()
        );
    }

    public static DamageAttributionProvenance registeredResolver(
            NexusId resolverId
    ) {
        return new DamageAttributionProvenance(
                DamageAttributionSource.REGISTERED_RESOLVER,
                Optional.of(Objects.requireNonNull(resolverId, "resolverId"))
        );
    }
}
