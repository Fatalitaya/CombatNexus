package io.github.fatalitaya.combatnexus.combat.projectile.platform;

import io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileDamageTarget;
import io.github.fatalitaya.combatnexus.combat.projectile.spi.NativeArrow;
import io.github.fatalitaya.combatnexus.combat.projectile.spi.NativeProjectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import java.util.Optional;

public record NativeArrowDamageTarget(AbstractArrow arrow) implements ProjectileDamageTarget {
    public NativeArrowDamageTarget { java.util.Objects.requireNonNull(arrow); }
    public Optional<LaunchIdentity> launch() {
        return ((NativeProjectile) arrow).combatnexus$launch().map(value ->
                new LaunchIdentity(value.action(), value.shooter().entity(), value.shooter().gameTick()));
    }
    public double damage() { return ((NativeArrow) arrow).combatnexus$baseDamage(); }
    public void damage(double value) { arrow.setBaseDamage(value); }
}
