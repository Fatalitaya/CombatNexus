package io.github.fatalitaya.combatnexus.effect.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** World-owned business operations; managed changes never reapply observed vanilla modifiers. */
public interface EffectService {
    boolean apply(EffectInstance effect, NexusId stacking);
    default boolean apply(EffectInstance effect, Stacking stacking) { return apply(effect, stacking.id()); }
    io.github.fatalitaya.combatnexus.combat.api.CommandResult<Boolean> applyInstant(EffectInstance effect);
    default boolean bind(EffectBinding binding) {
        if (binding.satisfied()) return apply(binding.effect(), binding.stacking());
        var state = binding.effect().snapshot();
        remove(state.target(), state.effect(), state.source(), Removal.CONDITION_ENDED);
        return true;
    }
    void remove(UUID target, NexusId effect, NexusId source, Removal reason);
    List<EffectInstance> snapshot(UUID target);
    List<EffectInstance> snapshot(UUID target, java.util.function.IntConsumer budget);
    boolean immune(UUID target,NexusId effect,Set<NexusId> tags,Set<ControlCategory> categories);
    List<EffectCleanse.Entry> cleanse(EffectCleanse request);
    int restrictionMask(UUID target);
    Set<NexusId> pendingSources(UUID target);

    enum Stacking {
        REPLACE, REFRESH_KEEP_STRONGER, STRONGER_WITH_HIDDEN;
        public NexusId id() { return NexusId.fromNamespaceAndPath("combatnexus", name().toLowerCase(java.util.Locale.ROOT)); }
    }
    enum Removal { DISPELLED, REMOVED, EXPIRED, CONDITION_ENDED, DEATH, RESPAWN, DIMENSION_CHANGE, WORLD_EXIT }
    /** before/after lists have zero or one element; empty after means revoke only the before source. */
    record Change(List<EffectInstance> before, List<EffectInstance> after, java.util.Optional<Removal> removal, boolean preservePeriodicPhase) {
        public Change(List<EffectInstance> before, List<EffectInstance> after, java.util.Optional<Removal> removal) { this(before, after, removal, false); }
        public Change {
            before = List.copyOf(before);
            after = List.copyOf(after);
            java.util.Objects.requireNonNull(removal, "removal");
        }
    }
}
