package io.github.fatalitaya.combatnexus.combat.health.platform.mixin;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment;
import io.github.fatalitaya.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMomentsMixin {
    @Shadow protected boolean dead;
    @Unique private boolean combatnexus$deathPublished;
    @Inject(method="die",at=@At("TAIL"),require=1)
    private void combatnexus$confirmedDeath(DamageSource source,CallbackInfo ci) {
        var target=(LivingEntity)(Object)this;
        if(!dead || combatnexus$deathPublished || !(target.level() instanceof ServerLevel level))return;
        combatnexus$deathPublished=true;
        EquipmentCombatAdapter.nativeDamage().confirmedDeath(target, source);
    }
    @Unique private int combatnexus$effectDepth;
    @Unique private Map<NexusId, CombatMoment.EffectState> combatnexus$effects() {
        if (((LivingEntity)(Object)this).getActiveEffects().isEmpty()) return Map.of();
        var result = new LinkedHashMap<NexusId, CombatMoment.EffectState>();
        for (var effect : ((LivingEntity)(Object)this).getActiveEffects()) {
            if (io.github.fatalitaya.combatnexus.effect.platform.NativeEffectBridge.managed(effect.getEffect())) continue;
            result.put(NativeIds.nexus(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value())), new CombatMoment.EffectState(effect.getDuration(), effect.getAmplifier()));
        }
        return result;
    }
    @Unique private void combatnexus$changes(Map<NexusId, CombatMoment.EffectState> before, Entity source, boolean tick) {
        var target = (LivingEntity)(Object)this;
        if (!(target.level() instanceof ServerLevel level)) return;
        var after = combatnexus$effects(); var ids = new LinkedHashSet<>(before.keySet()); ids.addAll(after.keySet());
        for (var id : ids) {
            var old = before.get(id); var next = after.get(id);
            if (Objects.equals(old, next) || tick && old != null && next != null && old.amplifier() == next.amplifier()) continue;
            var change = old == null ? CombatMoment.EffectChange.ADDED : next == null ? CombatMoment.EffectChange.REMOVED
                : next.amplifier() > old.amplifier() ? CombatMoment.EffectChange.UPGRADED
                : next.amplifier() == old.amplifier() ? CombatMoment.EffectChange.REFRESHED : CombatMoment.EffectChange.CHANGED;
            EquipmentCombatAdapter.effects().observedChange(level, new CombatMoment.Effect(target.getUUID(), id, Optional.ofNullable(old), Optional.ofNullable(next), change, Optional.ofNullable(source).map(Entity::getUUID), Optional.empty()));
        }
    }
    @WrapMethod(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean combatnexus$add(MobEffectInstance effect, Entity source, Operation<Boolean> original) {
        boolean outer = combatnexus$effectDepth++ == 0; var before = outer ? combatnexus$effects() : null;
        try { return original.call(effect, source); }
        finally { combatnexus$effectDepth--; if (outer) combatnexus$changes(before, source, false); }
    }
    @WrapMethod(method="forceAddEffect")
    private void combatnexus$force(MobEffectInstance effect, Entity source, Operation<Void> original) {
        boolean outer = combatnexus$effectDepth++ == 0; var before = outer ? combatnexus$effects() : null;
        try { original.call(effect, source); }
        finally { combatnexus$effectDepth--; if (outer) combatnexus$changes(before, source, false); }
    }
    @WrapMethod(method="removeEffect")
    private boolean combatnexus$remove(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, Operation<Boolean> original) {
        boolean outer = combatnexus$effectDepth++ == 0; var before = outer ? combatnexus$effects() : null;
        try { return original.call(effect); }
        finally { combatnexus$effectDepth--; if (outer) combatnexus$changes(before, null, false); }
    }
    @WrapMethod(method="removeAllEffects")
    private boolean combatnexus$clear(Operation<Boolean> original) {
        boolean outer = combatnexus$effectDepth++ == 0; var before = outer ? combatnexus$effects() : null;
        try { return original.call(); }
        finally { combatnexus$effectDepth--; if (outer) combatnexus$changes(before, null, false); }
    }
    @WrapMethod(method="tickEffects")
    private void combatnexus$tickEffects(Operation<Void> original) {
        boolean outer = combatnexus$effectDepth++ == 0; var before = outer ? combatnexus$effects() : null;
        try { original.call(); }
        finally { combatnexus$effectDepth--; if (outer) combatnexus$changes(before, null, true); }
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="applyItemBlocking", at=@At(value="INVOKE", target="Lnet/neoforged/neoforge/common/damagesource/DamageContainer;setBlockedDamage(Lnet/neoforged/neoforge/event/entity/living/LivingShieldBlockEvent;)V"), require=1)
    private void combatnexus$itemBlock(net.neoforged.neoforge.common.damagesource.DamageContainer container,
            net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event, Operation<Void> original) {
        var target = (LivingEntity)(Object)this;
        var item = NativeIds.nexus(BuiltInRegistries.ITEM.getKey(target.getUseItem().getItem()));
        var hand = target.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition.MAINHAND
                : io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition.OFFHAND;
        float incoming = container.getNewDamage();
        original.call(container, event);
        if (event.getBlocked() && !target.getUseItem().isEmpty() && container.getBlockedDamage() > 0) EquipmentCombatAdapter.nativeDamage().itemBlocked(target, container.getSource(),
                new io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockReceipt(
                        item, hand, incoming, container.getBlockedDamage(), container.getNewDamage(), false));
    }
}
