package io.github.fatalitaya.combatnexus.effect.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CombatWorldSessions;
import io.github.fatalitaya.combatnexus.effect.api.EffectFacts;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/** Reads native state at input capture; ownership merging remains in common. */
public final class NativeEffectFacts {
    private NativeEffectFacts() {}
    public static EffectFacts capture(LivingEntity entity) { return capture(entity, ignored -> {}); }
    public static EffectFacts capture(LivingEntity entity, IntConsumer budget) {
        var observed = entity.getActiveEffects().stream().map(effect -> {
            budget.accept(1);
            var holder = effect.getEffect();
            return new EffectFacts.Observed(NativeIds.nexus(holder.unwrapKey().orElseThrow().location()),
                    effect.getAmplifier(), effect.isInfiniteDuration() ? OptionalLong.empty() : OptionalLong.of(Math.max(0, effect.getDuration())),
                    NexusId.fromNamespaceAndPath("combatnexus", holder.value().getCategory().name().toLowerCase(Locale.ROOT)),
                    holder.tags().map(tag -> NativeIds.nexus(tag.location())).collect(Collectors.toUnmodifiableSet()));
        }).toList();
        var managed = entity.level() instanceof ServerLevel level
                ? CombatWorldSessions.managedEffects((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level), entity.getUUID(), budget).stream()
                    .map(effect -> new io.github.fatalitaya.combatnexus.effect.internal.EffectFactProjection.Managed(effect, CombatWorldSessions.source((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level), entity.getUUID(), effect.snapshot().source()))).toList()
                : List.<io.github.fatalitaya.combatnexus.effect.internal.EffectFactProjection.Managed>of();
        return io.github.fatalitaya.combatnexus.effect.internal.EffectFactProjection.merge(entity.getUUID(), (target, source) -> io.github.fatalitaya.combatnexus.combat.time.platform.NativeBusinessTime.now(entity, source), observed, managed);
    }
}
