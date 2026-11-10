package io.github.fatalitaya.combatnexus.compat.mixin.apotheosis;

import java.util.*;
import dev.shadowsoffire.apotheosis.socket.gem.*;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.BloodyArrowBonus;
import dev.shadowsoffire.apothic_attributes.api.AbilityCooldowns;
import io.github.fatalitaya.combatnexus.compat.apotheosis.GemSources;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileChange;
import io.github.fatalitaya.combatnexus.combat.state.api.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=BloodyArrowBonus.class, remap=false)
public abstract class BloodyArrowBonusMixin {
    @Shadow @Final protected Map<Purity,BloodyArrowBonus.Data> values;
    @Inject(method="onProjectileFired",at=@At("HEAD"),cancellable=true,require=1)
    private void combatnexus$launch(GemInstance instance, LivingEntity user, Projectile projectile, CallbackInfo callback) {
        if(!(user.level() instanceof ServerLevel level) || !(projectile instanceof AbstractArrow arrow)) return;
        var found=CombatAccess.find((io.github.fatalitaya.combatnexus.combat.api.WorldLookup) level); if(found.isEmpty()) return;
        callback.cancel();
        var world=found.orElseThrow();
        try(var call=io.github.fatalitaya.combatnexus.compat.BridgeCalls.replacing(world,"apotheosis:bloody_arrow",user.getUUID(),projectile.getUUID())) {
        var data=values.get(instance.purity());
        var source=new CombatSource(GemSources.identity(instance),Optional.of(user.getUUID()),Optional.empty());
        int cooldown=AbilityCooldowns.applyCDR(user,data.cooldown());
        if(AbilityCooldowns.isOnCooldown(user,io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(source.id()),data.cooldown()) || world.cooldowns().remaining(user.getUUID(),source.id(),cooldown)>0) { call.returnedNormally(); return; }
        var capture=world.projectiles().capture(UUID.randomUUID(),user.getUUID(),projectile);
        if(!capture.applied()) return;
        var launch=capture.value().orElseThrow();
        float cost=launch.shooter().facts().maxHealth()*data.healthCost();
        var damage=level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(BloodyArrowBonus.CORRUPTED)
                .map(DamageSource::new).orElseGet(()->user.damageSources().magic());
        // Cost, arrow write and cooldown share the launch parent's admission and derivation budget.
        var plan=new BeforeApplicationPlan(List.of(
                new BeforeApplicationPlan.Step(new io.github.fatalitaya.combatnexus.combat.damage.api.damage.NativeDamageCommand(UUID.randomUUID(),DamageRequest.builder(level,user,io.github.fatalitaya.combatnexus.combat.damage.platform.source.NativeDamageSources.from(damage),cost).build()),BeforeApplicationPlan.RejectionPolicy.CONTINUE),
                new BeforeApplicationPlan.Step(new DomainCommand.Projectile(new ProjectileChange(UUID.randomUUID(),user.getUUID(),source,new io.github.fatalitaya.combatnexus.combat.projectile.platform.NativeArrowDamageTarget(arrow),launch.action(),data.dmgMultiplier())),BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT),
                new BeforeApplicationPlan.Step(new DomainCommand.Cooldown(new CooldownCommand(UUID.randomUUID(),user.getUUID(),source,CooldownCommand.Operation.START,CooldownStamp.DeathPolicy.RETAIN)),BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT)));
        world.projectiles().execute(UUID.randomUUID(),user.getUUID(),projectile,launch.action(),plan);
        call.returnedNormally();
        }
    }
}
