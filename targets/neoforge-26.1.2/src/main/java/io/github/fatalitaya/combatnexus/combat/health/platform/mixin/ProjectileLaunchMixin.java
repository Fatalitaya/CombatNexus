package io.github.fatalitaya.combatnexus.combat.health.platform.mixin;

import java.util.Optional;
import io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch;
import io.github.fatalitaya.combatnexus.combat.projectile.spi.NativeProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.*;

@Mixin(Projectile.class)
public abstract class ProjectileLaunchMixin implements NativeProjectile {
    @Unique private ProjectileLaunch combatnexus$launch;
    @Unique private com.mojang.serialization.Dynamic<?> combatnexus$invalidLaunch;
    @Unique private com.mojang.serialization.Dynamic<?> combatnexus$rawSamples;
    @Unique private java.util.List<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample> combatnexus$samples = java.util.List.of();
    public java.util.List<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample> combatnexus$savedSamples() { return combatnexus$samples; }
    public void combatnexus$saveSamples(java.util.List<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample> samples) { combatnexus$samples = java.util.List.copyOf(samples); }
    public Optional<ProjectileLaunch> combatnexus$launch() {
        if (combatnexus$invalidLaunch != null) throw new IllegalStateException("Projectile launch restoration failed");
        return Optional.ofNullable(combatnexus$launch);
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = "addAdditionalSaveData", at = @org.spongepowered.asm.mixin.injection.At("TAIL"), require = 1)
    private void combatnexus$saveLaunch(net.minecraft.world.level.storage.ValueOutput output, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (combatnexus$invalidLaunch != null) {
            output.store("combatnexus_launch", com.mojang.serialization.Codec.PASSTHROUGH, combatnexus$invalidLaunch);
            if (combatnexus$rawSamples != null) output.store("combatnexus_launch_samples", com.mojang.serialization.Codec.PASSTHROUGH, combatnexus$rawSamples);
        } else if (combatnexus$launch != null) {
            io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectilePersistence.saving((Projectile)(Object)this);
            output.store("combatnexus_launch", io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectileLaunchCodec.CODEC, combatnexus$launch);
            output.store("combatnexus_launch_samples", io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectileLaunchCodec.SAMPLES, combatnexus$samples);
        }
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = "readAdditionalSaveData", at = @org.spongepowered.asm.mixin.injection.At("TAIL"), require = 1)
    private void combatnexus$loadLaunch(net.minecraft.world.level.storage.ValueInput input, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        combatnexus$launch = null; combatnexus$invalidLaunch = null; combatnexus$rawSamples = null; combatnexus$samples = java.util.List.of();
        var raw = input.read("combatnexus_launch", com.mojang.serialization.Codec.PASSTHROUGH);
        if (raw.isEmpty()) return;
        combatnexus$rawSamples = input.read("combatnexus_launch_samples", com.mojang.serialization.Codec.PASSTHROUGH).orElse(null);
        try {
            var launch = io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectileLaunchCodec.CODEC.parse(raw.orElseThrow()).getOrThrow();
            if (!launch.projectile().equals(((Projectile)(Object)this).getUUID())) throw new IllegalArgumentException("Saved projectile identity mismatch");
            combatnexus$samples = Optional.ofNullable(combatnexus$rawSamples)
                    .map(value -> io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectileLaunchCodec.SAMPLES.parse(value).getOrThrow()).orElse(java.util.List.of());
            combatnexus$launch = launch;
        } catch (RuntimeException failure) { combatnexus$invalidLaunch = raw.orElseThrow(); }
    }
    public void combatnexus$captureLaunch(ProjectileLaunch launch) {
        if (combatnexus$launch != null) throw new IllegalStateException("Projectile launch already captured");
        combatnexus$launch = launch;
    }
}
