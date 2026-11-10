package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation;
import java.util.*;

/** Source-scoped managed agility; source must distinguish each caster/cast instance. */
public final class ManagedEffects {
    private ManagedEffects() {}
    public static EffectInstance agility(UUID target, NexusId source, double speedBonus, long expiresAtTick) {
        if (!Double.isFinite(speedBonus) || speedBonus < 0 || speedBonus > 10 || expiresAtTick < 0) {
            throw new IllegalArgumentException("Invalid agility contribution");
        }
        var id = NexusId.fromNamespaceAndPath("combatnexus", "agility");
        return new EffectInstance(new EffectSnapshot(target, id, source, 0, OptionalLong.of(expiresAtTick), Set.of()),
                List.of(new EffectInstance.AttributeContribution(io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes.movementSpeed(),
                        new AttributeSnapshot.Contribution(id, speedBonus, AttributeOperation.ADD_MULTIPLIED_TOTAL))),
                io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED);
    }
}
