package io.github.fatalitaya.combatnexus.combat.internal.definition;

import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;

/** One publication boundary for the currently adopted server resource generation. */
public final class CombatDefinitions {
    private static final java.util.concurrent.atomic.AtomicLong GENERATIONS = new java.util.concurrent.atomic.AtomicLong();
    private CombatDefinitions() {}
    public static long nextGeneration() { return GENERATIONS.incrementAndGet(); }
    public static DefinitionGeneration forRuntime(CombatServerRuntime runtime) {
        return runtime.definitions().orElseThrow(() -> new IllegalStateException("Server definitions unavailable"));
    }
    public static synchronized void publish(CombatServerRuntime runtime, DefinitionGeneration candidate) {
        runtime.requireThread();
        if (runtime.definitions().orElse(null) == candidate) return;
        var previous = runtime.definitions();
        if (previous.isPresent() && candidate.generation() <= previous.get().generation())
            throw new IllegalArgumentException("Adopted generations must increase");
        runtime.adoptDefinitions(candidate);
        io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageModulePipeline.clearCache();
    }
}
