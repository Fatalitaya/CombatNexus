package io.github.fatalitaya.combatnexus.effect.api;
import java.util.*;
public record EffectApplication(UUID requestId, EffectInstance effect, io.github.fatalitaya.combatnexus.api.id.NexusId stacking) {
    public EffectApplication(UUID requestId, EffectInstance effect, EffectService.Stacking stacking) { this(requestId, effect, stacking.id()); }
    public EffectApplication force(){return new EffectApplication(requestId,effect.withApplication(EffectApplicationPolicy.FORCE),stacking);}
    public EffectApplication { Objects.requireNonNull(requestId); Objects.requireNonNull(effect); Objects.requireNonNull(stacking); }
}
