package io.github.fatalitaya.combatnexus.effect.api;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
/** Empty selectors match nothing. Categories and tags are alternative matches. */
public record EffectCleanse(UUID requestId,UUID target,Set<ControlCategory> categories,Set<NexusId> tags) {
    public EffectCleanse { Objects.requireNonNull(requestId);Objects.requireNonNull(target);categories=Set.copyOf(categories);tags=Set.copyOf(tags);if(tags.size()>32)throw new IllegalArgumentException("Cleanse tag capacity"); }
    public boolean matches(EffectInstance effect){return !Collections.disjoint(categories,effect.policy().control().categories())||!Collections.disjoint(tags,effect.policy().tags());}
    public record Entry(NexusId effect,NexusId source,boolean hidden,boolean removed) {}
}
