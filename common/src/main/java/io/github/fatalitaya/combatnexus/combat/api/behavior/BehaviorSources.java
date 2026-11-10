package io.github.fatalitaya.combatnexus.combat.api.behavior;
import java.util.*;
/** Read-only source facts. Activation and revocation belong to the Effect installation ledger. */
public interface BehaviorSources {
    List<BehaviorSource> capture(UUID target);
    List<BehaviorView> view(UUID target);
    boolean valid(BehaviorSource source);
}
