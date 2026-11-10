package io.github.fatalitaya.combatnexus.combat.damage.core.request;
import io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration;

import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageFailureReason;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageTriggerPolicy;
import io.github.fatalitaya.combatnexus.combat.config.CombatSafetySettings;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/** Atomic Phase 4 trigger, lineage, root, and server-tick admission. */
public final class DamageAdmissionController {

    private DamageAdmissionController() {
    }

    public static DamageAdmissionResult admitNative(
            DamageOrigin origin,
            CombatServerRuntime server, io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration definitions
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(server, "server");
        CombatSafetySettings settings = definitions.settings().safety();

        DamageFailureReason depthFailure = DamageExecutionPolicy.depthFailure(
                origin.lineage().recursionDepth(),
                settings.maxRecursionDepth()
        );
        if (depthFailure != null) {
            return rejectedWithoutConsumption(
                    origin,
                    server,
                    depthFailure
            );
        }

        var kernel = server;
        if (kernel.hasTicket(origin.lineage().transaction()))
            return DamageAdmissionResult.admitted(origin.lineage().derivedRequestCountInternal(), kernel.currentTickCount());
        throw new IllegalStateException("Native damage requires admission before entering hurtServer");
    }

    public static void clearServer(CombatServerRuntime server) {
        server.close();
    }

    public static DamageAdmissionResult rejectEventReentrancy(
            DamageOrigin origin,
            CombatServerRuntime server
    ) {
        return rejectedWithoutConsumption(
                Objects.requireNonNull(origin, "origin"),
                Objects.requireNonNull(server, "server"),
                DamageFailureReason.EVENT_REENTRANCY_LIMIT
        );
    }

    static int currentTickCount(CombatServerRuntime server) {
        return server.currentTickCount();
    }

    private static DamageAdmissionResult rejectedWithoutConsumption(
            DamageOrigin origin,
            CombatServerRuntime server,
            DamageFailureReason reason
    ) {
        return DamageAdmissionResult.rejected(
                reason,
                origin.lineage().derivedRequestCountInternal(),
                currentTickCount(server)
        );
    }

}
