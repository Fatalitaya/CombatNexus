package io.github.fatalitaya.combatnexus.compat.mixin.attributes;

import java.util.*;
import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.fatalitaya.combatnexus.combat.api.CombatAccess;
import io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaRequest;

@Mixin(value = AttributeEvents.class, remap = false)
public abstract class AttributeEventsMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "lifeStealOverheal", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D", ordinal = 0), require = 1)
    private double combatnexus$lifeSteal(LivingEntity attacker, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Double> original,
            net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        return io.github.fatalitaya.combatnexus.compat.apotheosis.AttributesBridge.manages(event.getSource()) && attacker.level() instanceof ServerLevel level && CombatAccess.find((io.github.fatalitaya.combatnexus.combat.api.WorldLookup) level).isPresent() ? 0 : original.call(attacker, attribute);
    }

    @Inject(method = "projDmg", at = @At("HEAD"), cancellable = true, require = 1)
    private void combatnexus$projectileDamage(LivingIncomingDamageEvent event, CallbackInfo callback) {
        if (event.getEntity().level() instanceof ServerLevel level
                && CombatAccess.find((io.github.fatalitaya.combatnexus.combat.api.WorldLookup) level).isPresent()
                && io.github.fatalitaya.combatnexus.compat.apotheosis.AttributesBridge.manages(event.getSource())) {
            callback.cancel();
        }
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "lifeStealOverheal", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D", ordinal = 1), require = 1)
    private double combatnexus$overheal(LivingEntity attacker, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Double> original,
            net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        double ratio = original.call(attacker, attribute);
        if (!(attacker.level() instanceof ServerLevel level)) return ratio;
        var world = CombatAccess.find((io.github.fatalitaya.combatnexus.combat.api.WorldLookup) level);
        if (world.isEmpty()) return ratio;
        try(var call=io.github.fatalitaya.combatnexus.compat.BridgeCalls.replacing(world.orElseThrow(),"apothic_attributes:overheal",attacker.getUUID(),io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileFacts.launch(event.getSource().getDirectEntity()).map(l->l.action()).or(()->world.orElseThrow().currentAction(attacker.getUUID()).map(a->a.id())).orElseGet(UUID::randomUUID))) {
        float damage = Math.min(event.getNewDamage(), event.getEntity().getData(ALObjects.Attachments.PRE_DAMAGE_HEALTH));
        if (ratio > 0) world.orElseThrow().absorption().submit(new io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand(
                UUID.randomUUID(), attacker.getUUID(), new io.github.fatalitaya.combatnexus.combat.api.CombatSource(
                    NexusId.fromNamespaceAndPath("apothic_attributes", "overheal"), Optional.of(attacker.getUUID()), Optional.empty()),
                io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand.Operation.GRANT,
                damage * (float)ratio, dev.shadowsoffire.apothic_attributes.util.AttributesUtil.getOverhealCap(attacker)));
        // Life steal is dispatched from the completed primary settlement; this branch owns Overheal only.
        call.returnedNormally();
        return 0;
        }
    }
    @Inject(method = "apothCriticalStrike", at = @At("HEAD"), cancellable = true, require = 1)
    private void combatnexus$critical(LivingIncomingDamageEvent event, CallbackInfo callback) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        var world = CombatAccess.find((io.github.fatalitaya.combatnexus.combat.api.WorldLookup) level);
        if (world.isEmpty()) return;
        callback.cancel();
        var launch=io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileFacts.launch(event.getSource().getDirectEntity());
        var attacker=event.getSource().getEntity() instanceof LivingEntity living?Optional.of(living.getUUID()):launch.map(l->l.shooter().entity());
        if (attacker.isEmpty() || event.getSource().is(ALObjects.Tags.CANNOT_CRITICALLY_STRIKE)) return;
        var action=launch.map(l->l.action()).or(()->world.orElseThrow().currentAction(attacker.orElseThrow()).map(a->a.id()));
        try(var call=io.github.fatalitaya.combatnexus.compat.BridgeCalls.replacing(world.orElseThrow(),"apothic_attributes:multi_critical",event.getEntity().getUUID(),action.orElseGet(UUID::randomUUID))) {
        var result = world.orElseThrow().damageFormula(new DamageFormulaRequest(UUID.randomUUID(),
                NexusId.fromNamespaceAndPath("apothic_attributes", "multi_critical"), event.getEntity().getUUID(),
                attacker, event.getAmount(), action, launch));
        if (result.output().amount() > event.getAmount()) PacketDistributor.sendToPlayersTrackingChunk(level,
                event.getEntity().chunkPosition(), new CritParticlePayload(event.getEntity().getId()));
        event.setAmount((float)result.output().amount());
        call.returnedNormally();
        }
    }
}
