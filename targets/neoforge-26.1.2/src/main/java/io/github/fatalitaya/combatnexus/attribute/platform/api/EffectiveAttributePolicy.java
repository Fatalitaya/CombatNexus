package io.github.fatalitaya.combatnexus.attribute.platform.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Capture-time policy. No callback is retained in a request or called during calculation. */
@FunctionalInterface
public interface EffectiveAttributePolicy {
    Optional<Reading> capture(LivingEntity entity, Holder<Attribute> attribute, double raw, double nativeEffective);

    record Reading(double effective, NexusId reason) {
        public Reading {
            Objects.requireNonNull(reason);
            if (!Double.isFinite(effective)) throw new IllegalArgumentException("Non-finite effective attribute");
        }
    }
}
