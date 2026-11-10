package io.github.fatalitaya.combatnexus.effect.internal;
import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.effect.api.EffectFacts.Observed;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import java.util.*;

/** Projects native observations and managed installations into detached facts. */
public final class EffectFactProjection {
    private EffectFactProjection() {}
    public record Managed(EffectInstance effect, Optional<SourceInstance> identity) {
        public Managed { Objects.requireNonNull(effect); Objects.requireNonNull(identity); }
    }
    public static EffectFacts merge(UUID target, io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time, List<Observed> observed, List<Managed> managed) {
        var effects = List.copyOf(observed);
        var merged = new ArrayList<EffectView>();
        for (var effect : effects) {
            if (managed.stream().anyMatch(e -> effect.id().equals(e.effect().display().nativeId()))) continue;
            merged.add(new EffectView(target, effect.id(), Optional.empty(), Optional.empty(), io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.VANILLA_OBSERVED,
                Set.of(effect.category()), effect.amplifier(), effect.remainingTicks(), Optional.empty(),
                EffectView.Completeness.UNKNOWN_SOURCE, Set.of(EffectView.Operation.DISPEL), SourceProvenance.UNKNOWN,EffectControl.NONE,EffectApplicationPolicy.NORMAL));
        }
        for (var entry : managed) {
            var effect = entry.effect();
            var state = effect.snapshot();
            var tags = new HashSet<>(effect.policy().tags());
            tags.add(NexusId.fromNamespaceAndPath("combatnexus", effect.policy().category().name().toLowerCase(Locale.ROOT)));
            var identity = entry.identity();
            merged.add(new EffectView(target, state.effect(), Optional.of(state.source()), identity.map(SourceInstance::instance),
                effect.ownership(), tags, state.level(), state.expiresAtTick().isEmpty() ? OptionalLong.empty()
                    : OptionalLong.of(Math.max(0, state.expiresAtTick().getAsLong() - time.now(target, state.source()))),
                Optional.ofNullable(effect.display().owner()).or(() -> identity.flatMap(SourceInstance::owner)),
                identity.isPresent() || !effect.display().provenance().equals(SourceProvenance.UNKNOWN) ? EffectView.Completeness.COMPLETE : EffectView.Completeness.UNKNOWN_SOURCE,
                effect.policy().dispellable() ? Set.of(EffectView.Operation.DISPEL) : Set.of(), effect.display().provenance(),effect.policy().control(),effect.policy().application()));
        }
        return new EffectFacts(effects, merged);
    }
}
