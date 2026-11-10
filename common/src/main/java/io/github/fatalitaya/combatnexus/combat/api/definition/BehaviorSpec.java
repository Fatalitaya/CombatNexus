package io.github.fatalitaya.combatnexus.combat.api.definition;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import java.util.*;
import io.github.fatalitaya.combatnexus.effect.api.EffectLifetime;
public record BehaviorSpec(int priority, EffectLifetime lifetime, List<BehaviorComponent> components) {
    public BehaviorSpec { components = new BehaviorDefinition(new BehaviorPlan(priority, components), lifetime).plan().components(); }
    public static final class Builder {
        private int priority; private EffectLifetime lifetime = EffectLifetime.timed(1200);
        private final List<BehaviorComponent> components = new ArrayList<>();
        public Builder priority(int value) { priority=value; return this; }
        public Builder duration(int ticks) { lifetime=EffectLifetime.timed(ticks); return this; }
        public Builder lifetime(EffectLifetime value) { lifetime=Objects.requireNonNull(value); return this; }
        public Builder component(BehaviorComponent value) { if(components.size()>=64) throw new IllegalArgumentException("Behavior component budget"); components.add(Objects.requireNonNull(value)); return this; }
        public BehaviorSpec build() { return new BehaviorSpec(priority,lifetime,components); }
    }
}
