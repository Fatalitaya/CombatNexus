package io.github.fatalitaya.combatnexus.combat.spi;

import io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession;

/** Read-only world identity installed by the target at world load. Null means unbound. */
public interface WorldRuntimeBinding extends io.github.fatalitaya.combatnexus.combat.api.WorldLookup {
    WorldSession combatnexus$worldSession();
    ServerRuntimeBinding combatnexus$serverBinding();
    private java.util.Optional<io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime> existing() {
        return io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(combatnexus$serverBinding())
            .flatMap(kernel -> kernel.world(combatnexus$worldSession()));
    }
    default java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.CombatWorld> combatWorld() {
        return existing().map(io.github.fatalitaya.combatnexus.combat.internal.WorldServices::new);
    }
    default java.util.List<io.github.fatalitaya.combatnexus.effect.api.EffectInstance> managedEffects(java.util.UUID target, java.util.function.IntConsumer budget) {
        return existing().map(world -> world.effects().snapshot(target, budget).stream().filter(effect->!world.effectControl().suspended(target,effect.snapshot().source())).toList()).orElse(java.util.List.of());
    }
    default java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.source.SourceInstance> source(java.util.UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId source) {
        return existing().flatMap(world -> world.sourceInstallations().identity(target, source));
    }
    default int restrictionMask(java.util.UUID target) {
        return existing().map(world -> world.effects().restrictionMask(target)).orElse(0);
    }
    default java.util.Optional<java.util.UUID> currentSession() {
        return existing().map(io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime::sessionId);
    }
}
