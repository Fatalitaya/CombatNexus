package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding;

/** Mutation is available only to target lifecycle wiring. */
public interface NativeServerRuntimeBinding extends ServerRuntimeBinding {
    void combatnexus$bindServerRuntime(CombatServerRuntime runtime);
}
