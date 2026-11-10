package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceInstance;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance;
import java.util.*;

/** Frozen observations merged with managed authority, without installing contributions. */
public final class EffectFacts {
    public record Observed(NexusId id, int amplifier, OptionalLong remainingTicks, NexusId category, Set<NexusId> tags) {
        public Observed {
            Objects.requireNonNull(id); Objects.requireNonNull(remainingTicks); Objects.requireNonNull(category);
            tags = Set.copyOf(tags);
        }
    }
    private final List<Observed> effects;
    private final List<EffectView> views;
    public EffectFacts(List<Observed> observed, List<EffectView> views) {
        this.effects = List.copyOf(observed); this.views = List.copyOf(views);
    }
    public List<EffectView> views() { return views; }
    public boolean hasEffect(NexusId definition) { return views.stream().anyMatch(e -> e.definition().equals(definition)); }
    public List<Observed> values() { return effects; }
    public boolean hasNativeEffect(NexusId effect) { return amplifier(effect).isPresent(); }
    public OptionalInt amplifier(NexusId effect) {
        return effects.stream().filter(value -> value.id().equals(effect)).mapToInt(Observed::amplifier).findFirst();
    }
}
