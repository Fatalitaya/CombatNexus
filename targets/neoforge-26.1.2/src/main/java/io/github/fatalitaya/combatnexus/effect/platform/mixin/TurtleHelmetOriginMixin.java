package io.github.fatalitaya.combatnexus.effect.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance;
import io.github.fatalitaya.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.core.component.DataComponents;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class TurtleHelmetOriginMixin {
    @WrapMethod(method = "turtleHelmetTick")
    private void combatnexus$origin(Operation<Void> original) {
        var player = (Player)(Object)this;
        if (player.level().isClientSide()) { original.call(); return; }
        for (var slot : EquipmentSlot.VALUES) {
            var stack = player.getItemBySlot(slot);
            var equippable = stack.get(DataComponents.EQUIPPABLE);
            if (stack.is(Items.TURTLE_HELMET) && equippable != null && equippable.slot() == slot) {
                EquipmentCombatAdapter.effectOrigins().invoke(player,
                    new SourceProvenance(player.getUUID(), NexusId.parse("minecraft:turtle_helmet"), io.github.fatalitaya.combatnexus.platform.NativeEquipment.position(slot), null, null),
                    () -> { original.call(); return null; });
                return;
            }
        }
        original.call();
    }
}
