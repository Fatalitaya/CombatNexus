package io.github.fatalitaya.combatnexus.attribute.platform;

import java.util.*;
import io.github.fatalitaya.combatnexus.attribute.platform.api.EffectiveAttributePolicy;
import net.minecraft.core.Holder;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Attribute-owned read policy table, published once by startup registration. */
public final class AttributeCapturePolicies {
    private static List<EffectiveAttributePolicy> policies = List.of();
    private static boolean installed;
    private AttributeCapturePolicies() {}
    public static final class Registration implements io.github.fatalitaya.combatnexus.attribute.platform.api.AttributeCaptureRegistrar, AutoCloseable {
        private final io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatListenerRegistry<EffectiveAttributePolicy> registered = new io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatListenerRegistry<>();
        private boolean open = true;
        public void register(NexusId id, int priority, EffectiveAttributePolicy policy) {
            if (!open) throw new IllegalStateException("Attribute capture registration closed");
            registered.register(id, priority, policy);
        }
        @Override public void close() {
            if (open) { open = false; registered.freeze(); }
        }
        public void publish() {
            if (open || installed) throw new IllegalStateException("Attribute policies must publish once after registration closes");
            policies = registered.entries().stream().map(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatListenerRegistry.Entry::listener).toList();
            installed = true;
        }
    }
    public static EffectiveAttributePolicy.Reading capture(LivingEntity entity, Holder<Attribute> attribute,
            double raw, double nativeEffective) {
        for (var policy : policies) {
            try (var read = io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
                var reading = policy.capture(entity, attribute, raw, nativeEffective);
                if (reading.isPresent()) return reading.get();
            } catch (Exception failure) {
            }
        }
        return new EffectiveAttributePolicy.Reading(nativeEffective, NexusId.withDefaultNamespace("entity_attribute_value"));
    }
}
