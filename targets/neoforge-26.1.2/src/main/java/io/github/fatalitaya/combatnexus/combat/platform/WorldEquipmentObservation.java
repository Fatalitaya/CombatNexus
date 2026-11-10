package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.api.behavior.EquipmentItem;
import io.github.fatalitaya.combatnexus.combat.api.platform.EquipmentObservation;
import io.github.fatalitaya.combatnexus.platform.NativeEquipment;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.UUID;

/** Native reads bound to the world lifecycle that owns the equipment ledger. */
public final class WorldEquipmentObservation implements EquipmentObservation {
    private final ServerLevel level;
    public WorldEquipmentObservation(ServerLevel level) { this.level = java.util.Objects.requireNonNull(level); }
    public boolean alive(UUID target) {
        return level.getEntity(target) instanceof LivingEntity living && living.isAlive();
    }
    public Optional<EquipmentItem> item(UUID target, EquipmentPosition slot) {
        return level.getEntity(target) instanceof LivingEntity living
                ? Optional.of(capture(level, living.getItemBySlot(NativeEquipment.slot(slot)))) : Optional.empty();
    }
    public static EquipmentItem capture(ServerLevel level, ItemStack stack) {
        var item=NativeIds.nexus(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        try {
            var bindings=stack.isEmpty()?CombatItemBindingSpec.EMPTY:io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding)level.getServer())).bindings(new io.github.fatalitaya.combatnexus.combat.damage.platform.item.NativeRuleItem(stack));
            return new EquipmentItem(item,bindings,Optional.empty());
        }catch(IllegalArgumentException failure){return new EquipmentItem(item,CombatItemBindingSpec.EMPTY,Optional.of(failure.toString()));}
    }
}
