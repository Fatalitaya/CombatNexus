package io.github.fatalitaya.combatnexus.effect.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Unified detached facts. Unknown instance/owner attribution is explicit; observation never installs contributions. */
public record EffectView(UUID target, NexusId definition, Optional<NexusId> source, Optional<UUID> sourceInstance,
        io.github.fatalitaya.combatnexus.effect.api.EffectOwnership ownership, Set<NexusId> tags, int level, OptionalLong remainingTicks,
        Optional<UUID> owner, Completeness completeness, Set<Operation> operations, io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance provenance,EffectControl control,EffectApplicationPolicy application) {
    public enum Completeness { COMPLETE, UNKNOWN_SOURCE }
    public enum Operation { DISPEL }
    public EffectView {
        Objects.requireNonNull(target); Objects.requireNonNull(definition); Objects.requireNonNull(source);
        Objects.requireNonNull(sourceInstance); Objects.requireNonNull(ownership); Objects.requireNonNull(remainingTicks);
        Objects.requireNonNull(provenance); Objects.requireNonNull(owner); Objects.requireNonNull(completeness);
        Objects.requireNonNull(control);Objects.requireNonNull(application);
        tags = Set.copyOf(tags); operations = Set.copyOf(operations);
    }
    public boolean hasTag(NexusId tag) { return tags.contains(tag); }
}
