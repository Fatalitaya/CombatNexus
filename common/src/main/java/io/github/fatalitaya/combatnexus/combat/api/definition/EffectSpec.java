package io.github.fatalitaya.combatnexus.combat.api.definition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.effect.api.*;
import java.util.*;
/** References are resolved only after Java and pack declarations have been selected. */
public record EffectSpec(int level, EffectLifetime lifetime, NexusId stacking, EffectPolicy policy,
        List<EffectInstance.AttributeContribution> attributes, List<NexusId> behaviors, int priority,
        Set<EffectSnapshot.RestrictedAction> restrictions, EffectDisplay display) {
    public EffectSpec {
        Objects.requireNonNull(lifetime); Objects.requireNonNull(stacking); Objects.requireNonNull(policy); Objects.requireNonNull(display);
        attributes=List.copyOf(attributes); behaviors=List.copyOf(behaviors); restrictions=Set.copyOf(restrictions);
        if(level<0 || level>255 || priority < -10000 || priority>10000 || attributes.size()>32 || behaviors.size()>32 || new HashSet<>(behaviors).size()!=behaviors.size()) throw new IllegalArgumentException("Invalid effect declaration");
    }
    public static final class Builder {
        private int level, priority; private EffectLifetime lifetime=EffectLifetime.permanent();
        private NexusId stacking=EffectService.Stacking.REPLACE.id(); private EffectPolicy policy=EffectPolicy.DEFAULT;
        private EffectDisplay display=EffectDisplay.NONE;
        private final List<EffectInstance.AttributeContribution> attributes=new ArrayList<>();
        private final List<NexusId> behaviors=new ArrayList<>();
        private final Set<EffectSnapshot.RestrictedAction> restrictions=EnumSet.noneOf(EffectSnapshot.RestrictedAction.class);
        public Builder level(int value){level=value;return this;}
        public Builder priority(int value){priority=value;return this;}
        public Builder lifetime(EffectLifetime value){lifetime=value;return this;}
        public Builder stacking(NexusId value){stacking=value;return this;}
        public Builder policy(EffectPolicy value){policy=value;return this;}
        public Builder display(EffectDisplay value){display=value;return this;}
        public Builder attribute(EffectInstance.AttributeContribution value){if(attributes.size()>=32)throw new IllegalArgumentException("Attribute budget");attributes.add(value);return this;}
        public Builder behavior(NexusId value){if(behaviors.size()>=32)throw new IllegalArgumentException("Behavior reference budget");behaviors.add(value);return this;}
        public Builder restrict(EffectSnapshot.RestrictedAction value){if(!restrictions.add(value))throw new IllegalArgumentException("Duplicate restriction");return this;}
        public EffectSpec build(){return new EffectSpec(level,lifetime,stacking,policy,attributes,behaviors,priority,restrictions,display);}
    }
}
