package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation;

import io.github.fatalitaya.combatnexus.combat.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.registry.PreMultiplierBuckets;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Optional;

final class DamageOperationPreMultiplierBuckets {

    private DamageOperationPreMultiplierBuckets() {
    }

    static int resolveOrGeneric(Optional<NexusId> bucketId) {
        PreMultiplierBucketRegistry.requireFrozen();

        if (bucketId == null || bucketId.isEmpty()) {
            return PreMultiplierBuckets.genericDamage();
        }

        return PreMultiplierBucketRegistry.getPreMultiplierBucketIdOrUnknown(
                bucketId.get()
        );
    }
}
