package io.github.fatalitaya.combatnexus.combat.damage.platform.mixin;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fatalitaya.combatnexus.combat.platform.EquipmentCombatAdapter;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Wolf.class)
public abstract class WolfArmorMixin {
    @Shadow private boolean canArmorAbsorb(DamageSource source) { throw new AssertionError(); }
    @WrapMethod(method = "actuallyHurt")
    private void combatnexus$equipmentReceipt(DamageSource source, float amount, Operation<Void> original) {
        var wolf = (Wolf)(Object)this;
        if (!canArmorAbsorb(source)) { original.call(source, amount); return; }
        EquipmentCombatAdapter.access().apply(wolf, source, receipt -> {
            var armor = wolf.getBodyArmorItem();
            var item = NativeIds.nexus(BuiltInRegistries.ITEM.getKey(armor.getItem()));
            int before = armor.getDamageValue(), maximum = armor.getMaxDamage();
            boolean completed = false;
            try { original.call(source, amount); completed = true; }
            finally {
                boolean broken = armor.isEmpty();
                int consumed = Math.max(0, (broken ? maximum : armor.getDamageValue()) - before);
                receipt.accept(new EquipmentDamageReceipt(io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition.BODY, item, completed ? amount : 0, consumed, broken));
            }
        });
    }
}
