package io.github.fatalitaya.combatnexus.effect.api;

import java.util.UUID;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Adopted display identity and source attribution; never a second timer. */
public record EffectDisplay(NexusId nativeId, boolean ambient, boolean particles, boolean icon,
        UUID owner, boolean aggregateSources, boolean persistent, boolean environment,
        io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance provenance) {
    public EffectDisplay(NexusId nativeId, boolean ambient, boolean particles, boolean icon,
            UUID owner, boolean aggregateSources, boolean persistent, boolean environment) {
        this(nativeId, ambient, particles, icon, owner, aggregateSources, persistent, environment,
            io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance.entity(owner));
    }
    public EffectDisplay withProvenance(io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance source) {
        return new EffectDisplay(nativeId, ambient, particles, icon, source.entity(), aggregateSources, persistent, environment, source);
    }
    public EffectDisplay { java.util.Objects.requireNonNull(provenance); }
    public static final EffectDisplay NONE = new EffectDisplay(null, false, false, false, null, false, false, false);
}
