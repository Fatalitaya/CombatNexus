package io.github.fatalitaya.combatnexus.combat.damage.api.context;

import io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts;
import io.github.fatalitaya.combatnexus.api.entity.EntityCategory;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Frozen rule inputs; neither entity access nor mutation authority. */
public record DamageParticipantFacts(EntityTypeFacts entityType, EntityCategory category,
        float health, float maxHealth, boolean onFire, Set<NexusId> effects,
        Set<NexusId> nativeEffects, Set<NexusId> nativeEffectTags) {
    public DamageParticipantFacts {
        Objects.requireNonNull(entityType); Objects.requireNonNull(category);
        effects = Set.copyOf(effects); nativeEffects = Set.copyOf(nativeEffects); nativeEffectTags = Set.copyOf(nativeEffectTags);
    }
    public boolean hasEffect(NexusId effect) { return effects.contains(effect); }
    public boolean hasNativeEffect(NexusId effect) { return nativeEffects.contains(effect); }
}
