package io.github.fatalitaya.combatnexus.combat.damage.core.settlement;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ServerSession;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageParentRef;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementStatus;
import io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementCallback;
import io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementListener;
import io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle.DamageModuleRegistrationAccess;
import io.github.fatalitaya.combatnexus.combat.damage.core.util.JvmFatalErrors;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import org.jetbrains.annotations.ApiStatus;

import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatListenerRegistry;
import java.util.List;
import java.util.Objects;

/** Frozen registry and per-listener dynamic authority boundary. */
public final class DamageSettlementCallbacks {
    @ApiStatus.Internal
    public static final int MIN_PRIORITY = -10_000;
    @ApiStatus.Internal
    public static final int MAX_PRIORITY = 10_000;

    private static final CombatListenerRegistry<DamageSettlementListener> REGISTRY = new CombatListenerRegistry<>();

    private DamageSettlementCallbacks() {
    }

    public static synchronized void register(
            DamageModuleRegistrationAccess access,
            NexusId id,
            int priority,
            DamageSettlementListener listener
    ) {
        DamageModuleLifecycle.requireRegistering(
                access, "registerSettlementListener");
        REGISTRY.register(id, priority, listener);
    }

    public static synchronized void freeze(
            DamageModuleRegistrationAccess access
    ) {
        DamageModuleLifecycle.requireRegistering(
                access, "freezeSettlementListeners");
        REGISTRY.freeze();
    }

    static void dispatch(DamageSettlementCompletion completion) {
        var snapshot = completion.snapshot();
        Objects.requireNonNull(snapshot, "snapshot");
        var currentEntries = REGISTRY.entries();
        if (currentEntries.isEmpty()) {
            return;
        }
        ServerSession serverIdentity = completion.serverSession();
        if (serverIdentity == null) {
            throw new IllegalStateException(
                    "Settlement callbacks require the authoritative server thread"
            );
        }

        boolean grantsChildAuthority = snapshot.status()
                == DamageSettlementStatus.APPLIED;
        for (var entry : currentEntries) {
            DamageParentRef authority = grantsChildAuthority
                    ? createAuthorityForInvocation(completion.origin())
                    : null;
            DamageSettlementCallback callback =
                    DamageSettlementCallback.createInternal(
                            snapshot, authority, serverIdentity);
            try (DamageSettlementDispatchScope.Scope ignored =
                         DamageSettlementDispatchScope.openCallback(
                                 callback, authority, serverIdentity)) {
                entry.listener().onDamageSettled(callback);
            } catch (Throwable throwable) {
                JvmFatalErrors.rethrowIfFatal(throwable);
            }
        }
    }

    static DamageParentRef createAuthorityForInvocation(
            DamageOrigin parentOrigin
    ) {
        return DamageParentRef.createInternal(parentOrigin);
    }

    @ApiStatus.Internal
    public static List<NexusId> orderedIds() {
        return REGISTRY.frozen() ? REGISTRY.entries().stream().map(CombatListenerRegistry.Entry::id).toList() : List.of();
    }

    @ApiStatus.Internal
    public static synchronized void resetForTesting() {
        REGISTRY.clear();
    }

}
