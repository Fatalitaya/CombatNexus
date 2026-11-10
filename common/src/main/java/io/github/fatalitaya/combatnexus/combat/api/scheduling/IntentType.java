package io.github.fatalitaya.combatnexus.combat.api.scheduling;
import java.util.*;
import java.util.function.BiConsumer;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
/** Startup registration for immutable, serializable intent parameters. */
public record IntentType<T extends ScheduledIntent>(NexusId id, Class<T> implementation, MapCodec<T> codec,
        Set<CombatOperation> permissions, BiConsumer<T, IntentExecution> executor) {
    public IntentType { Objects.requireNonNull(id); Objects.requireNonNull(implementation); Objects.requireNonNull(codec); permissions=Set.copyOf(permissions); Objects.requireNonNull(executor); }
}
