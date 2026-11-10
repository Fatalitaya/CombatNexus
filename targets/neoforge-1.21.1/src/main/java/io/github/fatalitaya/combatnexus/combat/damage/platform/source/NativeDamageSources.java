package io.github.fatalitaya.combatnexus.combat.damage.platform.source;

import io.github.fatalitaya.combatnexus.api.geometry.Position3;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSourceDescriptor;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;
import java.util.Objects;
import java.util.Optional;

/** Native damage source translation, with no registry lookup or synthetic identities. */
public final class NativeDamageSources {
    private NativeDamageSources() {}

    public static DamageSourceDescriptor from(DamageSource source) {
        return tryFrom(source).orElseThrow(() -> new IllegalArgumentException("Damage source type is not registry-backed"));
    }
    public static Optional<DamageSourceDescriptor> tryFrom(DamageSource source) {
        Objects.requireNonNull(source, "Damage source must not be null");
        return source.typeHolder().unwrapKey().map(key -> {
            var position = source.sourcePositionRaw();
            return position == null ? of(key) : positioned(key, position);
        });
    }
    public static DamageSourceDescriptor of(ResourceKey<DamageType> key) {
        return DamageSourceDescriptor.of(NativeIds.nexus(Objects.requireNonNull(key).location()));
    }
    public static DamageSourceDescriptor positioned(ResourceKey<DamageType> key, Vec3 position) {
        return DamageSourceDescriptor.positioned(NativeIds.nexus(Objects.requireNonNull(key).location()),
                new Position3(position.x, position.y, position.z));
    }
    public static DamageSource create(DamageOrigin origin, Holder<DamageType> type) {
        var position = origin.source().sourcePosition().map(p -> new Vec3(p.x(), p.y(), p.z())).orElse(null);
        return new DamageSource(type, origin.attribution().directEntity(), origin.attribution().logicalAttacker(), position);
    }
}
