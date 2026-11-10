package io.github.fatalitaya.combatnexus.combat.spi;

import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import java.util.UUID;

/** Trusted native entry adapter; no nested admission authority is exposed. */
public final class NativeHealingAccess {
    private NativeHealingAccess() {}
    public enum Route { MANAGED, REJECTED_PHASE }
    public static Route route() {
        try { CombatExecutionScope.requireSubmissionPhase(); return Route.MANAGED; }
        catch (IllegalStateException invalid) { return Route.REJECTED_PHASE; }
    }
    public static HealingSettlement submit(CombatRuntime world, ParticipantIdentity target, float amount, NativeHealingSource source) {
        if (route() != Route.MANAGED) throw new IllegalStateException("Native healing is not at a permitted entry");
        return world.healing().submit(new HealingRequest(UUID.randomUUID(), target.entity(), source.source(), source.reason(), amount, source.source().owner(), java.util.Optional.of(target)));
    }
}
