package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionTarget;
import io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionWorldAccess;
import io.github.fatalitaya.combatnexus.combat.health.platform.NativeAbsorptionTarget;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;
import java.util.UUID;

public final class WorldAbsorptionAccess implements AbsorptionWorldAccess {
    private final ServerLevel level;
    public WorldAbsorptionAccess(ServerLevel level) { this.level = java.util.Objects.requireNonNull(level); }
    public Optional<AbsorptionTarget> find(UUID id) {
        return level.getEntity(id) instanceof LivingEntity living ? Optional.of(new NativeAbsorptionTarget(living)) : Optional.empty();
    }
}
