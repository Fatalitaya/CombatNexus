package io.github.fatalitaya.combatnexus.attribute.spi;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.Optional;
/** Stateless native operations, installed explicitly by the loader composition root. */
public final class AttributeAccess {
    private AttributeAccess() {}
    private static Port port;
    public static void install(Port implementation) {
        if (port != null) throw new IllegalStateException("AttributeAccess already installed");
        port = java.util.Objects.requireNonNull(implementation);
    }
    public static Port get() { return java.util.Objects.requireNonNull(port, "AttributeAccess not installed"); }
    public interface Port {
        boolean exists(NexusId id);
        String descriptionId(NexusId id);
        double sanitize(NexusId attribute, double value);
        NexusId attributeId(Attribute attribute);
        Optional<Holder.Reference<Attribute>> attribute(NexusId id);
        NexusId modifierId(AttributeModifier modifier);
        io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation operation(AttributeModifier modifier);
        AttributeModifier modifier(NexusId id, double amount, io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation operation);
        AttributeModifier find(AttributeInstance attribute, NexusId id);
        void remove(AttributeInstance attribute, NexusId id);
    }
}
