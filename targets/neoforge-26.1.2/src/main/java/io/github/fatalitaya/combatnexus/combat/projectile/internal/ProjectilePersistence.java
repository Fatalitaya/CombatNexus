package io.github.fatalitaya.combatnexus.combat.projectile.internal;

import io.github.fatalitaya.combatnexus.combat.projectile.spi.NativeProjectile;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.spi.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;

/** Native entity persistence and lifecycle, separate from read-only projectile queries. */
public final class ProjectilePersistence {
    private ProjectilePersistence() {}
    public static io.github.fatalitaya.combatnexus.combat.internal.coordination.DecisionSamples.Owner owner(net.minecraft.world.entity.Entity projectile) {
        return new io.github.fatalitaya.combatnexus.combat.internal.coordination.DecisionSamples.Owner(projectile.getUUID(),((ParticipantInstance)projectile).combatnexus$incarnation());
    }
    public static void saving(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        var nativeProjectile = (NativeProjectile) projectile;
        nativeProjectile.combatnexus$launch().ifPresent(launch ->
            CombatServerRuntime.existing((ServerRuntimeBinding) level.getServer())
                .flatMap(kernel -> kernel.world(((WorldRuntimeBinding) level).combatnexus$worldSession()))
                .filter(world -> world.decisionSamples().contains(launch.action()))
                .ifPresent(world -> nativeProjectile.combatnexus$saveSamples(world.decisionSamples().snapshot(launch.action()))));
    }
    public static void joined(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        var nativeProjectile = (NativeProjectile) projectile;
        nativeProjectile.combatnexus$launch().ifPresent(launch ->
            io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(level)
                .decisionSamples().restore(launch.action(), owner(projectile), nativeProjectile.combatnexus$savedSamples()));
    }
}
