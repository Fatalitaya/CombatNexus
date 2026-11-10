package io.github.fatalitaya.combatnexus.combat.damage.core.util;

import org.jetbrains.annotations.ApiStatus;

/** Shared boundary for errors that must never be isolated or logged away. */
@ApiStatus.Internal
public final class JvmFatalErrors {

    private JvmFatalErrors() {
    }

    public static void rethrowIfFatal(Throwable throwable) {
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatFailures.rethrowFatal(throwable);
    }
}
