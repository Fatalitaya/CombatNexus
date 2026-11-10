package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.List;
import java.util.Objects;

/** Observed vanilla modifiers are descriptive only; managed modifiers need one platform executor. */
public record EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, EffectOwnership ownership, io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorPlan behavior, EffectPolicy policy, List<EffectInstance> hidden, EffectDisplay display, NexusId stacking) {
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, EffectOwnership ownership,
            io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorPlan behavior, EffectPolicy policy, List<EffectInstance> hidden, EffectDisplay display) {
        this(snapshot, attributes, ownership, behavior, policy, hidden, display, EffectService.Stacking.REPLACE.id());
    }
    public EffectInstance withApplication(EffectApplicationPolicy application){return new EffectInstance(snapshot,attributes,ownership,behavior,policy.withApplication(application),hidden,display,stacking);}
    public EffectInstance withStacking(NexusId strategy) { return new EffectInstance(snapshot, attributes, ownership, behavior, policy, hidden, display, strategy); }
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, EffectOwnership ownership,
            io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorPlan behavior, EffectPolicy policy, List<EffectInstance> hidden) {
        this(snapshot, attributes, ownership, behavior, policy, hidden, EffectDisplay.NONE);
    }
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, EffectOwnership ownership,
            io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorPlan behavior, EffectPolicy policy) {
        this(snapshot, attributes, ownership, behavior, policy, List.of());
    }
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, EffectOwnership ownership, io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorPlan behavior) {
        this(snapshot, attributes, ownership, behavior, EffectPolicy.DEFAULT);
    }
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, EffectOwnership ownership) {
        this(snapshot, attributes, ownership, io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorPlan.NONE);
    }
    public EffectInstance {
        Objects.requireNonNull(stacking);
        Objects.requireNonNull(display);
        Objects.requireNonNull(policy);
        Objects.requireNonNull(behavior);
        if (ownership == EffectOwnership.VANILLA_OBSERVED && !behavior.empty()) throw new IllegalArgumentException("Observed effect cannot own behavior");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(ownership, "ownership");
        attributes = List.copyOf(attributes);
        hidden = List.copyOf(hidden);
        if (hidden.size() > 256 || hidden.stream().anyMatch(e -> !e.hidden().isEmpty()
                || !e.snapshot().target().equals(snapshot.target()) || !e.snapshot().effect().equals(snapshot.effect())
                || (!display.aggregateSources() && !e.snapshot().source().equals(snapshot.source())) || e.snapshot().lifetime() == EffectLifetime.Kind.INSTANT))
            throw new IllegalArgumentException("Invalid hidden effect layers");
        if (snapshot.lifetime() == EffectLifetime.Kind.INSTANT) {
            if (!hidden.isEmpty() || !attributes.isEmpty() || !snapshot.restrictions().isEmpty()
                    || behavior.components().stream().anyMatch(c -> !c.instant()))
                throw new IllegalArgumentException("Instant effects can only contain instant behaviors");
        } else if (behavior.components().stream().anyMatch(c -> c.instant())) {
            throw new IllegalArgumentException("Instant behaviors require an instant effect");
        }
        if (ownership == EffectOwnership.VANILLA_OBSERVED && !snapshot.restrictions().isEmpty()) {
            throw new IllegalArgumentException("Observed effects cannot install managed restrictions");
        }
    }
    public record AttributeContribution(NexusId attribute, AttributeSnapshot.Contribution modifier) {
        public AttributeContribution {
            Objects.requireNonNull(attribute, "attribute");
            Objects.requireNonNull(modifier, "modifier");
        }
    }
}
