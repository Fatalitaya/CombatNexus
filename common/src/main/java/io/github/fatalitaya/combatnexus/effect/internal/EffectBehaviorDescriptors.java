package io.github.fatalitaya.combatnexus.effect.internal;

import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent;
import io.github.fatalitaya.combatnexus.combat.internal.registration.BehaviorTypes;
import io.github.fatalitaya.combatnexus.effect.api.EffectBehaviorDescriptor;
import io.github.fatalitaya.combatnexus.presentation.api.TooltipBlock;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

public final class EffectBehaviorDescriptors {
    private record Binding<T extends BehaviorComponent>(Class<T> type, EffectBehaviorDescriptor<T> descriptor) {
        TooltipBlock describe(BehaviorComponent value, int level) { return Objects.requireNonNull(descriptor.describe(type.cast(value), level)); }
    }
    private static final Map<NexusId, Binding<?>> BINDINGS = new LinkedHashMap<>();
    private static boolean frozen;
    private static java.util.function.Function<BehaviorComponent,List<DisplayText>> periodic;
    public static synchronized void installPeriodicDetails(java.util.function.Function<BehaviorComponent,List<DisplayText>> provider) {
        if(frozen || periodic != null) throw new IllegalStateException("Periodic descriptions already installed or frozen");
        periodic=Objects.requireNonNull(provider);
    }
    public static List<DisplayText> periodicDetails(BehaviorComponent value) {
        if(periodic==null)throw new IllegalStateException("Periodic descriptions not installed");
        return List.copyOf(periodic.apply(value));
    }
    private EffectBehaviorDescriptors() {}
    public static synchronized <T extends BehaviorComponent> void register(NexusId id, Class<T> type, EffectBehaviorDescriptor<T> descriptor) {
        if (frozen) throw new IllegalStateException("Effect descriptors frozen");
        if (BehaviorTypes.registered(id).implementation() != type) throw new IllegalArgumentException("Effect descriptor type mismatch: " + id);
        if (BINDINGS.putIfAbsent(Objects.requireNonNull(id), new Binding<>(Objects.requireNonNull(type), Objects.requireNonNull(descriptor))) != null)
            throw new IllegalArgumentException("Duplicate effect descriptor: " + id);
    }
    public static synchronized void freeze() {
        BINDINGS.forEach((id, binding) -> {
            if (BehaviorTypes.registered(id).implementation() != binding.type()) throw new IllegalStateException("Effect descriptor type mismatch: " + id);
        });
        frozen = true;
    }
    public static TooltipBlock describe(BehaviorComponent value, int level) {
        var type = BehaviorTypes.type(value);
        var binding = BINDINGS.get(type.id());
        if (binding == null) return new TooltipBlock(java.util.Optional.<DisplayText>empty(), List.of(GeneratedTexts.translatable("tooltip.combatnexus.effect.unknown_behavior")), List.of());
        if (binding.type() != value.getClass()) throw new IllegalStateException("Effect descriptor type mismatch: " + type.id());
        return binding.describe(value, level);
    }
}
