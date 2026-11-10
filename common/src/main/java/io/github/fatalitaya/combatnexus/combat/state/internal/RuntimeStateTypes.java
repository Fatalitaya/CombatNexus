package io.github.fatalitaya.combatnexus.combat.state.internal;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.state.api.StateKey;
public final class RuntimeStateTypes {
    private RuntimeStateTypes() {}
    private static Map<NexusId,StateKey<?>> types=Map.of();
    public static void install(Map<NexusId,StateKey<?>> values) { types=Map.copyOf(values); }
    public static StateKey<?> get(NexusId id) { return Objects.requireNonNull(types.get(id),"Unregistered runtime state: "+id); }
    public static void require(StateKey<?> key) { if(get(key.id())!=key)throw new IllegalArgumentException("State definition identity mismatch"); }
}
