package io.github.fatalitaya.combatnexus.effect.api;

import java.util.Objects;

/** A sampled condition owns one stable (target, effect, source) installation. */
public record EffectBinding(EffectInstance effect, boolean satisfied, io.github.fatalitaya.combatnexus.api.id.NexusId stacking) {
    public EffectBinding(EffectInstance effect, boolean satisfied, EffectService.Stacking stacking) { this(effect, satisfied, stacking.id()); }
    public EffectBinding {
        Objects.requireNonNull(effect); Objects.requireNonNull(stacking);
        if (effect.ownership() != io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED
                || effect.snapshot().lifetime() == EffectLifetime.Kind.INSTANT)
            throw new IllegalArgumentException("A condition binding requires a managed active effect");
    }
}
