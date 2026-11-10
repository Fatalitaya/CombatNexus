package io.github.fatalitaya.combatnexus.combat.internal.transaction;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope;

import java.util.*;
import java.util.function.Consumer;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Deterministic listener registry. Freeze explicitly before use; one failing listener cannot undo a commit. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatListeners<T> {
    private final Object owner = new Object();
    private final CombatListenerRegistry<Consumer<CombatCallback<T>>> registry = new CombatListenerRegistry<>();
    public void register(NexusId id, int priority, Consumer<CombatCallback<T>> listener) {
        registry.register(id, priority, listener);
    }
    public void dispatch(ServerSession server, CombatLineage lineage, T result, Set<CombatOperation> allowed) {
        if (!registry.frozen()) throw new IllegalStateException("Listener table was not frozen during initialization");
        for (var entry : registry.entries()) {
            var callback = new CombatCallback<>(lineage, result, server);
            try (var ignored = CombatDispatchScope.callback(owner, callback, null, server, lineage, allowed)) {
                entry.listener().accept(callback);
            } catch (Throwable failure) {
                CombatFailures.rethrowFatal(failure);
            }
        }
    }
    public void freeze() { registry.freeze(); }
    public void clear() { registry.clear(); }
}
