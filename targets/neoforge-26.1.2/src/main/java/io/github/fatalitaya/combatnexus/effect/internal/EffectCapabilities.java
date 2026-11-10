package io.github.fatalitaya.combatnexus.effect.internal;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.effect.api.*;

/** Startup registry shared by native and custom effect plans. */
public final class EffectCapabilities {
    private static final Map<NexusId, EffectCapability> VALUES = new LinkedHashMap<>();
    private static boolean frozen;
    private EffectCapabilities() {}
    public static synchronized void register(NexusId id, EffectCapability capability) {
        if (frozen) throw new IllegalStateException("Effect capabilities frozen");
        if (VALUES.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(capability)) != null)
            throw new IllegalArgumentException("Duplicate effect capability " + id);
        EffectCapabilityDescriptions.register(id, capability::describe);
    }
    public static java.util.List<io.github.fatalitaya.combatnexus.presentation.api.DisplayText> describeRequired(NexusId id, int amplifier) {
        var value = VALUES.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown effect description: " + id);
        return value.describe(amplifier);
    }
    public static void freeze() { EffectCapabilityDescriptions.freeze(); frozen = true; }
    public static boolean execute(NexusId id, EffectCapability.Context context) {
        var capability = VALUES.get(id);
        if (capability == null) throw new IllegalArgumentException("Unknown effect capability " + id);
        return capability.execute(context);
    }
    public static java.util.List<io.github.fatalitaya.combatnexus.presentation.api.DisplayText> describe(NexusId id, int amplifier) {
        var value = VALUES.get(id);
        return value == null ? java.util.List.of(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.effect.unknown_behavior")) : value.describe(amplifier);
    }
}
