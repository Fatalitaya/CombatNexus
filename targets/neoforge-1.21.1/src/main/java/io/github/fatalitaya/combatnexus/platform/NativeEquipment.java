package io.github.fatalitaya.combatnexus.platform;

import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import net.minecraft.world.entity.EquipmentSlot;

/** Explicit translation at native equipment boundaries; null denotes unknown provenance. */
public final class NativeEquipment {
    public static java.util.Set<EquipmentPosition> positions() {
        return java.util.Arrays.stream(EquipmentSlot.values()).map(NativeEquipment::position).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    private NativeEquipment() {}

    public static EquipmentPosition position(EquipmentSlot slot) {
        if (slot == null) return null;
        return switch (slot) {
            case MAINHAND -> EquipmentPosition.MAINHAND;
            case OFFHAND -> EquipmentPosition.OFFHAND;
            case FEET -> EquipmentPosition.FEET;
            case LEGS -> EquipmentPosition.LEGS;
            case CHEST -> EquipmentPosition.CHEST;
            case HEAD -> EquipmentPosition.HEAD;
            case BODY -> EquipmentPosition.BODY;
        };
    }

    public static EquipmentSlot slot(EquipmentPosition position) {
        return switch (position) {
            case MAINHAND -> EquipmentSlot.MAINHAND;
            case OFFHAND -> EquipmentSlot.OFFHAND;
            case FEET -> EquipmentSlot.FEET;
            case LEGS -> EquipmentSlot.LEGS;
            case CHEST -> EquipmentSlot.CHEST;
            case HEAD -> EquipmentSlot.HEAD;
            case BODY -> EquipmentSlot.BODY;
            case SADDLE -> throw new IllegalArgumentException("Saddle is not an equipment slot in Minecraft 1.21.1");
        };
    }
}
