package io.github.fatalitaya.combatnexus.compat;

import java.util.UUID;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.bridge.*;

public final class BridgeCalls {
    private BridgeCalls() {}
    public static BridgeCall replacing(CombatWorld world, String id, UUID target, UUID action) {
        var capability=CombatAccess.bridgeCoverage().stream().filter(c->c.id().equals(NexusId.parse(id))&&c.mode()==BridgeCapability.Mode.REPLACE).findFirst().orElseThrow();
        var call=world.bridges().open(UUID.randomUUID(),action,target,capability.id(),capability.position(),capability.executor());
        call.nativeContribution(BridgeInvocation.Contribution.SUPPRESSED);
        return call;
    }
}
