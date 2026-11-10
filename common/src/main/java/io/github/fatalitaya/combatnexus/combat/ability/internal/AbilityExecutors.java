package io.github.fatalitaya.combatnexus.combat.ability.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.ability.api.AbilityExecutor;
import java.util.*;

/** Code callbacks and inert metadata are frozen together at loader startup. */
public final class AbilityExecutors {
    private static final Map<NexusId,AbilityExecutor> VALUES=new LinkedHashMap<>();
    private static boolean frozen;
    private AbilityExecutors() {}
    public static void register(NexusId id, AbilityExecutor executor) {
        if(frozen)throw new IllegalStateException("Ability executors frozen");
        Objects.requireNonNull(id);Objects.requireNonNull(executor);
        if(VALUES.containsKey(id)||VALUES.size()>=256)throw new IllegalArgumentException("Duplicate ability executor or capacity");
        AbilityExecutorCatalog.register(id,executor.spec()); VALUES.put(id,executor);
    }
    public static void freeze(){AbilityExecutorCatalog.freeze();frozen=true;}
    public static AbilityExecutor require(NexusId id){return Objects.requireNonNull(VALUES.get(id),"Unregistered ability executor: "+id);}
}
