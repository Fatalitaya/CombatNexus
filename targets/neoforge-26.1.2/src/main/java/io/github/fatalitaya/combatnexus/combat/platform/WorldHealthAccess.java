package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.combat.api.platform.*;
import io.github.fatalitaya.combatnexus.combat.health.api.LethalAttempt;
import io.github.fatalitaya.combatnexus.combat.health.platform.NativeHealthBinding;
import io.github.fatalitaya.combatnexus.combat.health.platform.NativeLethalBinding;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;
import java.util.UUID;

public final class WorldHealthAccess implements HealthWorldAccess {
    private final ServerLevel level;
    public WorldHealthAccess(ServerLevel level) { this.level = java.util.Objects.requireNonNull(level); }
    public HealthTarget target(UUID entity) { return new NativeHealthBinding((LivingEntity)level.getEntity(entity)); }
    public Optional<LethalTarget> lethal(LethalAttempt command) {
        var target = (LivingEntity)level.getEntity(command.target());
        var type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(ResourceKey.create(Registries.DAMAGE_TYPE, NativeIds.nativeId(command.damageType())));
        return type.map(holder -> new NativeLethalBinding(target, new DamageSource(holder, command.attacker().map(level::getEntity).orElse(null))));
    }
}
