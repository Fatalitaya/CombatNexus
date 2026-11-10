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
    private record Declaration(long incarnation, long generation, boolean empty,
            io.github.fatalitaya.combatnexus.api.id.NexusId item,
            CombatItemBindingSpec defaults,
            io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingPatch patch) {}
    private record Observation(Declaration declaration, EquipmentItem resolved) {}
    private final java.util.Map<UUID, java.util.Map<EquipmentPosition, Observation>> observations = new java.util.HashMap<>();
    public Optional<EquipmentItem> item(UUID target, EquipmentPosition slot) {
        if (!NativeEquipment.positions().contains(slot)) return Optional.empty();
        if (!(level.getEntity(target) instanceof LivingEntity living)) { clear(target); return Optional.empty(); }
        var stack = living.getItemBySlot(NativeEquipment.slot(slot));
        var generation = io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level).definitions().generation();
        var declaration = new Declaration(((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance)living).combatnexus$incarnation(),
            generation, stack.isEmpty(), NativeIds.nexus(BuiltInRegistries.ITEM.getKey(stack.getItem())),
            io.github.fatalitaya.combatnexus.combat.api.item.CombatItems.defaults(stack),
            io.github.fatalitaya.combatnexus.combat.api.item.CombatItems.patch(stack));
        var slots = observations.computeIfAbsent(target, ignored -> new java.util.EnumMap<>(EquipmentPosition.class));
        var previous = slots.get(slot);
        if (previous != null && previous.declaration().equals(declaration)) return Optional.of(previous.resolved());
        var observed = capture(level, stack);
        slots.put(slot, new Observation(declaration, observed));
        return Optional.of(observed);
    }
    @Override public void clear(UUID target) { observations.remove(target); }
    @Override public void close() { observations.clear(); }
    public static EquipmentItem capture(ServerLevel level, ItemStack stack) {
        var item=NativeIds.nexus(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        try {
            var bindings=stack.isEmpty()?CombatItemBindingSpec.EMPTY:io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding)level.getServer())).bindings(new io.github.fatalitaya.combatnexus.combat.damage.platform.item.NativeRuleItem(stack));
            return new EquipmentItem(item,bindings,Optional.empty());
        }catch(IllegalArgumentException failure){return new EquipmentItem(item,CombatItemBindingSpec.EMPTY,Optional.of(failure.toString()));}
    }
}
