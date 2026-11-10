package io.github.fatalitaya.combatnexus.effect.api;

import java.util.*;

/** Immutable strongest-first layers; suppressed durations continue to elapse. */
public final class EffectLayers {
    private EffectLayers() {}
    private static long deadline(EffectInstance effect) { return effect.snapshot().expiresAtTick().orElse(Long.MAX_VALUE); }
    private static EffectInstance flat(EffectInstance e) {
        return new EffectInstance(e.snapshot(), e.attributes(), e.ownership(), e.behavior(), e.policy(), List.of(), e.display(), e.stacking());
    }
    public static EffectInstance merge(EffectInstance previous, EffectInstance incoming, long now) {
        if (!previous.snapshot().target().equals(incoming.snapshot().target())
                || !previous.snapshot().effect().equals(incoming.snapshot().effect())
                || (!(previous.display().aggregateSources() && incoming.display().aggregateSources()) && !previous.snapshot().source().equals(incoming.snapshot().source())))
            throw new IllegalArgumentException("Cannot merge unrelated effect identities");
        var candidates = new ArrayList<EffectInstance>();
        candidates.add(flat(previous)); candidates.addAll(previous.hidden());
        candidates.add(flat(incoming)); candidates.addAll(incoming.hidden());
        var selected = select(candidates, now).orElseThrow();
        if (!previous.display().aggregateSources()) return selected;
        var oldDisplay = previous.display(); var input = incoming.display(); var display = selected.display();
        boolean changed = incoming.snapshot().level() > previous.snapshot().level()
            || incoming.snapshot().level() == previous.snapshot().level() && deadline(incoming) > deadline(previous);
        return new EffectInstance(selected.snapshot(), selected.attributes(), selected.ownership(), selected.behavior(), selected.policy(), selected.hidden(),
            new EffectDisplay(display.nativeId(), changed ? input.ambient() : oldDisplay.ambient() && input.ambient(),
                input.particles(), input.icon(), display.owner(), display.aggregateSources(), display.persistent(), display.environment(), display.provenance()), incoming.stacking());
    }
    public static Optional<EffectInstance> active(EffectInstance effect, long now) {
        var candidates = new ArrayList<EffectInstance>();
        candidates.add(flat(effect)); candidates.addAll(effect.hidden());
        return select(candidates, now);
    }
    public static Optional<EffectInstance> withoutSource(EffectInstance effect, io.github.fatalitaya.combatnexus.api.id.NexusId source, long now) {
        return select(java.util.stream.Stream.concat(java.util.stream.Stream.of(flat(effect)), effect.hidden().stream())
            .filter(layer -> !layer.snapshot().source().equals(source)).toList(), now);
    }
    private static Optional<EffectInstance> select(List<EffectInstance> candidates, long now) {
        var selected = io.github.fatalitaya.combatnexus.effect.internal.EffectLayerSelection.select(
                candidates.stream().map(value -> new io.github.fatalitaya.combatnexus.effect.internal.EffectLayerSelection.Candidate(value.snapshot().level(), deadline(value))).toList(), now);
        var retained = new ArrayList<EffectInstance>();
        for (int index : selected) retained.add(flat(candidates.get(index)));
        if (retained.isEmpty()) return Optional.empty();
        var head = retained.remove(0);
        return Optional.of(new EffectInstance(head.snapshot(), head.attributes(), head.ownership(), head.behavior(), head.policy(), retained, head.display(), head.stacking()));
    }
    public static EffectInstance rebase(EffectInstance effect, long savedTick, long now) {
        var s = effect.snapshot();
        var expiry = io.github.fatalitaya.combatnexus.effect.internal.EffectLayerSelection.rebase(s.expiresAtTick(), savedTick, now);
        return new EffectInstance(new EffectSnapshot(s.target(), s.effect(), s.source(), s.level(), expiry, s.restrictions(), s.lifetime()),
                effect.attributes(), effect.ownership(), effect.behavior(), effect.policy(),
                effect.hidden().stream().map(e -> rebase(e, savedTick, now)).toList(), effect.display(), effect.stacking());
    }
    /** Translate each layer independently when layers are owned by different source clocks. */
    public static EffectInstance reclock(EffectInstance effect,
            io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime from,
            io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime to) {
        var s=effect.snapshot();
        var expiry=s.expiresAtTick().isEmpty()?OptionalLong.empty():OptionalLong.of(Math.addExact(
            to.now(s.target(),s.source()),Math.max(0,s.expiresAtTick().getAsLong()-from.now(s.target(),s.source()))));
        return new EffectInstance(new EffectSnapshot(s.target(),s.effect(),s.source(),s.level(),expiry,s.restrictions(),s.lifetime()),
            effect.attributes(),effect.ownership(),effect.behavior(),effect.policy(),
            effect.hidden().stream().map(e->reclock(e,from,to)).toList(),effect.display(),effect.stacking());
    }
    public static Optional<EffectInstance> active(EffectInstance effect,io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) {
        io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime zero=(target,source)->0;
        return active(reclock(effect,time,zero),0).map(value->reclock(value,zero,time));
    }
}
