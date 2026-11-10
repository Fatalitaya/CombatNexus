package io.github.fatalitaya.combatnexus.effect.internal;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.effect.api.*;

public final class EffectStackingStrategies {
    private static final Map<NexusId, EffectStackingStrategy> VALUES = new LinkedHashMap<>();
    private static boolean frozen;
    static {
        register(EffectService.Stacking.REPLACE.id(), (before, incoming, now) -> incoming);
        register(EffectService.Stacking.STRONGER_WITH_HIDDEN.id(), EffectLayers::merge);
        register(EffectService.Stacking.REFRESH_KEEP_STRONGER.id(), (before, incoming, now) -> {
            var b = before.snapshot(); var s = incoming.snapshot();
            var strongest = b.level() > s.level() ? before : incoming;
            var selected = strongest.snapshot();
            var expiry = b.expiresAtTick().isEmpty() || s.expiresAtTick().isEmpty() ? OptionalLong.empty()
                : OptionalLong.of(Math.max(b.expiresAtTick().getAsLong(), s.expiresAtTick().getAsLong()));
            return new EffectInstance(new EffectSnapshot(selected.target(), selected.effect(), selected.source(), selected.level(), expiry, selected.restrictions()),
                strongest.attributes(), strongest.ownership(), strongest.behavior(), strongest.policy(), strongest.hidden(), strongest.display());
        });
    }
    private EffectStackingStrategies() {}
    public static synchronized void register(NexusId id, EffectStackingStrategy strategy) {
        if (frozen) throw new IllegalStateException("Effect stacking registry frozen");
        if (VALUES.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(strategy)) != null)
            throw new IllegalArgumentException("Duplicate stacking strategy " + id);
    }
    public static void freeze() { frozen = true; }
    public static EffectStackingStrategy get(NexusId id) {
        var value = VALUES.get(id); if (value == null) throw new IllegalArgumentException("Unknown effect stacking strategy " + id); return value;
    }
}
