package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.platform.DamageTypeCatalog;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class WorldDamageTypes implements DamageTypeCatalog {
    private final ServerLevel level;
    public WorldDamageTypes(ServerLevel level) { this.level = java.util.Objects.requireNonNull(level); }
    public boolean contains(NexusId type) {
        return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .get(ResourceKey.create(Registries.DAMAGE_TYPE, NativeIds.nativeId(type))).isPresent();
    }
    public Optional<Set<NexusId>> tag(NexusId tag) {
        return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .get(TagKey.create(Registries.DAMAGE_TYPE, NativeIds.nativeId(tag)))
                .map(values -> values.stream().map(holder -> NativeIds.nexus(holder.unwrapKey().orElseThrow().identifier()))
                        .collect(Collectors.toUnmodifiableSet()));
    }
}
