package io.github.fatalitaya.combatnexus.combat.spi;

import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import java.util.Optional;

/** Read-only binding supplied by the target. Reading it never creates a runtime. */
public interface ServerRuntimeBinding {
    Optional<CombatServerRuntime> combatnexus$serverRuntime();
}
