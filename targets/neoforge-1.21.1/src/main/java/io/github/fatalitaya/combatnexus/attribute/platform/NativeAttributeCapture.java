package io.github.fatalitaya.combatnexus.attribute.platform;

import io.github.fatalitaya.combatnexus.attribute.api.*;
import io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import java.util.ArrayList;
import java.util.function.IntConsumer;

/** Native observation only; snapshot assembly and revision publication belong to common. */
public final class NativeAttributeCapture {
    private NativeAttributeCapture() {}
    public static AttributeObservation observe(LivingEntity entity, Iterable<Holder<Attribute>> requested, IntConsumer budget, java.util.Map<io.github.fatalitaya.combatnexus.api.id.NexusId, java.util.List<AttributeSnapshot.Contribution>> previous) {
        if (entity.level().isClientSide()) throw new IllegalArgumentException("Server snapshot required");
        var entries = new ArrayList<AttributeObservation.Entry>();
        for (var holder : requested) {
            budget.accept(1);
            var instance = entity.getAttribute(holder);
            if (instance == null) continue;
            var id = NativeIds.nexus(holder.unwrapKey().orElseThrow().location());
            var modifiers = instance.getModifiers();
            var contributions = previous.get(id);
            boolean same = contributions != null && contributions.size() == modifiers.size();
            int index = 0;
            for (var modifier : modifiers) {
                budget.accept(1);
                if (same) {
                    var old = contributions.get(index);
                    same = old.source().equals(AttributeAccess.get().modifierId(modifier))
                        && Double.compare(old.amount(), modifier.amount()) == 0
                        && old.operation() == AttributeAccess.get().operation(modifier);
                }
                index++;
            }
            if (!same) {
                var changed = new ArrayList<AttributeSnapshot.Contribution>(modifiers.size());
                for (var modifier : modifiers) changed.add(new AttributeSnapshot.Contribution(
                    AttributeAccess.get().modifierId(modifier), modifier.amount(), AttributeAccess.get().operation(modifier)));
                contributions = java.util.List.copyOf(changed);
            }
            double raw = instance.getValue();
            var reading = AttributeCapturePolicies.capture(entity, holder, raw, entity.getAttributeValue(holder));
            entries.add(new AttributeObservation.Entry(id, new AttributeValueFrame.Value(instance.getBaseValue(), raw, reading.effective(), reading.reason()), contributions));
        }
        return new AttributeObservation(entity.getUUID(), entity.level().getGameTime(), entries);
    }
}
