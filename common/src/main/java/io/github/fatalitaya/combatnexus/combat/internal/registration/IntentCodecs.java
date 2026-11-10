package io.github.fatalitaya.combatnexus.combat.internal.registration;

import java.util.*;
import com.mojang.serialization.*;
import io.github.fatalitaya.combatnexus.api.id.*;
import io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent;

public final class IntentCodecs {
    public record Type<T extends ScheduledIntent>(NexusId id, Class<T> implementation, MapCodec<T> codec) {
        public Type { Objects.requireNonNull(id); Objects.requireNonNull(implementation); Objects.requireNonNull(codec); }
    }
    private static final Map<NexusId, Type<?>> TYPES = new LinkedHashMap<>();
    private static final Map<Class<?>, Type<?>> CLASSES = new HashMap<>();
    private static boolean frozen;
    public static final Codec<ScheduledIntent> CODEC = NexusIdCodecs.CODEC.dispatch("type", v -> type(v).id(), id -> {
        var t = TYPES.get(id); if (t == null) throw new IllegalArgumentException("Unknown intent: " + id); return t.codec();
    });
    private IntentCodecs() {}
    public static <T extends ScheduledIntent> void register(Type<T> t) {
        if (frozen) throw new IllegalStateException("Intent registry frozen");
        if (TYPES.containsKey(t.id()) || CLASSES.containsKey(t.implementation())) throw new IllegalArgumentException("Duplicate intent: " + t.id());
        TYPES.put(t.id(), t); CLASSES.put(t.implementation(), t);
    }
    public static List<Type<?>> types() { return List.copyOf(TYPES.values()); }
    public static void freeze() { frozen = true; }
    public static Type<?> type(ScheduledIntent value) { return Objects.requireNonNull(CLASSES.get(value.getClass()), "Unregistered intent"); }
}
