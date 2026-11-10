package io.github.fatalitaya.combatnexus.combat.api.bridge;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
public interface BridgeCalls {
    BridgeCall open(UUID invocation, UUID action, UUID target, NexusId capability, NexusId position, NexusId executor);
    List<BridgeInvocation> history();
}
