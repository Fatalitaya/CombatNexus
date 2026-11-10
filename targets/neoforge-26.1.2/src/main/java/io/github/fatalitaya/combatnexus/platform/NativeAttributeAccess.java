package io.github.fatalitaya.combatnexus.platform;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.Optional;
import io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess;
import net.minecraft.core.registries.*;
public final class NativeAttributeAccess implements AttributeAccess.Port, io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Port {
    public NexusId vanilla(io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.Vanilla attribute) {
        return NativeIds.nexus((switch (attribute) {
            case ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED;
            case ARMOR -> Attributes.ARMOR;
            case ARMOR_TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
            case MAX_HEALTH -> Attributes.MAX_HEALTH;
            case MAX_ABSORPTION -> Attributes.MAX_ABSORPTION;
        }).unwrapKey().orElseThrow().identifier());
    }
    public static Holder<Attribute> resolve(NexusId id) { return BuiltInRegistries.ATTRIBUTE.get(NativeIds.nativeId(id)).orElseThrow(); }
    public String descriptionId(NexusId id) { return resolve(id).value().getDescriptionId(); }
    public double sanitize(NexusId id, double value) { return resolve(id).value().sanitizeValue(value); }
    public boolean exists(NexusId id) { return attribute(id).isPresent(); }
    public NexusId attributeId(Attribute attribute) { return NativeIds.nexus(BuiltInRegistries.ATTRIBUTE.getKey(attribute)); }
    public Optional<Holder.Reference<Attribute>> attribute(NexusId id) { return BuiltInRegistries.ATTRIBUTE.get(NativeIds.nativeId(id)); }
    public NexusId modifierId(AttributeModifier modifier) { return NativeIds.nexus(modifier.id()); }
    public AttributeModifier modifier(NexusId id, double amount, io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation operation) { return new AttributeModifier(NativeIds.nativeId(id), amount, nativeOperation(operation)); }
    public io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation operation(AttributeModifier modifier) {
        return switch (modifier.operation()) {
            case ADD_VALUE -> io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation.ADD_MULTIPLIED_TOTAL;
        };
    }
    private static AttributeModifier.Operation nativeOperation(io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation operation) {
        return switch (operation) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
    public AttributeModifier find(AttributeInstance attribute, NexusId id) { return attribute.getModifier(NativeIds.nativeId(id)); }
    public void remove(AttributeInstance attribute, NexusId id) { attribute.removeModifier(NativeIds.nativeId(id)); }
}
