package io.github.fatalitaya.combatnexus.combat.damage.api.event;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageParentRef;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import io.github.fatalitaya.combatnexus.combat.damage.core.settlement.DamageSettlementCallbacks;
import io.github.fatalitaya.combatnexus.combat.damage.core.settlement.DamageSettlementDispatchScope;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only invocation object for a registered
 * {@link DamageSettlementListener}.
 *
 * <p>The child authority is visible only while this exact invocation is
 * running on its owning server. Every registered listener invocation receives
 * a distinct authority object even when all listeners observe the same parent
 * settlement. Saving this object or its authority does not extend that
 * lifetime.</p>
 */
public final class DamageSettlementCallback {

    private final DamageSettlementSnapshot snapshot;
    private final io.github.fatalitaya.combatnexus.combat.api.transaction.ServerSession server;
    private final @Nullable DamageParentRef authority;

    private DamageSettlementCallback(
            DamageSettlementSnapshot snapshot,
            @Nullable DamageParentRef authority, io.github.fatalitaya.combatnexus.combat.api.transaction.ServerSession server
    ) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.authority = authority; this.server = server;
    }

    /** Framework-only construction boundary. */
    @ApiStatus.Internal
    public static DamageSettlementCallback createInternal(
            DamageSettlementSnapshot snapshot,
            @Nullable DamageParentRef authority, io.github.fatalitaya.combatnexus.combat.api.transaction.ServerSession server
    ) {
        if (StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass() != DamageSettlementCallbacks.class) {
            throw new SecurityException(
                    "Only the settlement callback dispatcher may create an invocation"
            );
        }
        return new DamageSettlementCallback(snapshot, authority, server);
    }

    /** Cross-domain children share the root budget; absent when this damage suppresses procs. */
    public Optional<io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent> combatAuthority() {
        return io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope.parentFor(
                this, server);
    }

    public DamageSettlementContext context() { return DamageSettlementContext.current(snapshot); }

    public DamageSettlementSnapshot snapshot() {
        return snapshot;
    }

    /**
     * Returns authority for independent derived requests only during this
     * exact callback invocation. NOT_APPLIED settlements return empty.
     */
    public Optional<DamageParentRef> childAuthority() {
        DamageParentRef candidate = authority;
        if (candidate == null || !DamageSettlementDispatchScope.exposesAuthority(
                this,
                candidate,
                server
        )) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }
}
