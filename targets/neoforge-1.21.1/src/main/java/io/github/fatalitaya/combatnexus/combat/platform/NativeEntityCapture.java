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
        return capture(entity, ignored -> {}, true);
    }
    public static CombatEntityFacts captureFacts(LivingEntity entity, java.util.function.IntConsumer budget) {
        return capture(entity, budget, false).facts();
    }
    private static Captured capture(LivingEntity entity, java.util.function.IntConsumer budget, boolean nativeStacks) {
        var content = io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get();
        var category = content.entityCategory(entity.getType());
        var entityType = content.entityTypeFacts(entity.getType());
        int chargeTicks = entity.getTicksUsingItem();
        float attackStrength = entity instanceof net.minecraft.world.entity.player.Player player ? player.getAttackStrengthScale(0.5F) : 1;
        float health = entity.getHealth(), maxHealth = entity.getMaxHealth();
        boolean onFire = entity.isOnFire();
        var equipment = new EnumMap<EquipmentPosition, CapturedItem>(EquipmentPosition.class);
        var values = new EnumMap<EquipmentPosition, ItemFacts>(EquipmentPosition.class);
        var world = entity.level() instanceof net.minecraft.server.level.ServerLevel level
            ? io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level) : null;
        var selection = world == null ? null : world.equipmentSelection(entity.getUUID());
        for (var slot : EquipmentSlot.values()) {
            var position = EquipmentPosition.valueOf(slot.name());
            var stack = entity.getItemBySlot(slot);
            var facts = new ItemFacts(stack.isEmpty(), content.itemId(stack.getItem()), stack.getCount(),
                io.github.fatalitaya.combatnexus.combat.api.item.CombatItems.defaults(stack),
                io.github.fatalitaya.combatnexus.combat.api.item.CombatItems.patch(stack), Optional.empty(), Optional.empty());
            var selected = selection == null ? facts : selection.capture(facts, position);
            values.put(position, selected);
            if (nativeStacks) equipment.put(position, new CapturedItem(stack, selected.defaultBindings(), selected.entries(), selected.affixes()));
        }
        var weaponSlot = !ItemStack.isSameItemSameComponents(entity.getWeaponItem(), entity.getMainHandItem())
            && ItemStack.isSameItemSameComponents(entity.getWeaponItem(), entity.getOffhandItem()) && !entity.getOffhandItem().isEmpty()
            ? EquipmentPosition.OFFHAND : EquipmentPosition.MAINHAND;
        var effects = content.captureEffects(entity, budget);
        var external = io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.captureFacts(entity);
        var facts = new CombatEntityFacts(entityType, category, health, maxHealth, onFire,
            external,
            values.get(weaponSlot), chargeTicks, attackStrength,
            values, io.github.fatalitaya.combatnexus.platform.NativeEquipment.positions(), effects);
        return new Captured(facts, equipment.get(weaponSlot), equipment);
    }
}
