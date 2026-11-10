package io.github.fatalitaya.combatnexus.combat.platform;
import io.github.fatalitaya.combatnexus.combat.api.input.*;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Native stacks have exactly the capture/request lifetime; only facts enter common. */
public final class NativeEntityCapture {
    private NativeEntityCapture() {}
    public record Captured(CombatEntityFacts facts, CapturedItem weapon, Map<EquipmentPosition, CapturedItem> equipment) {
        public Captured { equipment = Map.copyOf(equipment); }
    }
    public record Participant(CombatInputFrame frame, Captured nativeFacts) {}
    public static Captured capture(LivingEntity entity) {
        var category = io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().entityCategory(entity.getType());
        var entityType = io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().entityTypeFacts(entity.getType());
        int chargeTicks = entity.getTicksUsingItem();
        float attackStrength = entity instanceof net.minecraft.world.entity.player.Player player ? player.getAttackStrengthScale(0.5F) : 1;
        float health = entity.getHealth(), maxHealth = entity.getMaxHealth();
        boolean onFire = entity.isOnFire();
        var equipment = new EnumMap<EquipmentPosition, CapturedItem>(EquipmentPosition.class);
        for (var slot : EquipmentSlot.values()) equipment.put(EquipmentPosition.valueOf(slot.name()), new CapturedItem(entity.getItemBySlot(slot)));
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
            var world = io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level);
            if (world != null) { var selection = world.equipmentSelection(entity.getUUID()); equipment.replaceAll((slot,item) -> {
                var selected = selection.capture(item.facts(), slot);
                var stack = item.copyStack(); stack.remove(CombatDataComponents.PATCH.get()); stack.remove(CombatDataComponents.BINDINGS.get());
                return new CapturedItem(stack, selected.defaultBindings(), selected.entries(), selected.affixes());
            }); }
        }
        var weaponSlot = !ItemStack.isSameItemSameComponents(entity.getWeaponItem(),entity.getMainHandItem()) && ItemStack.isSameItemSameComponents(entity.getWeaponItem(),entity.getOffhandItem()) && !entity.getOffhandItem().isEmpty() ? EquipmentPosition.OFFHAND : EquipmentPosition.MAINHAND;
        var weapon = equipment.get(weaponSlot);
        var effects = io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().captureEffects(entity, ignored -> {});
        var external = io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.captureFacts(entity);
        var values = new EnumMap<EquipmentPosition, ItemFacts>(EquipmentPosition.class);
        equipment.forEach((slot,item) -> values.put(slot, item.facts()));
        return new Captured(new CombatEntityFacts(entityType, category, health, maxHealth, onFire, external, weapon.facts(), chargeTicks, attackStrength, values, effects), weapon, equipment);
    }
}
