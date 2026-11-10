package io.github.fatalitaya.combatnexus.combat.action.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import io.github.fatalitaya.combatnexus.api.geometry.Vector3;
public sealed interface EntityCommand {
    UUID requestId(); UUID target(); CombatSource source();
    record Ignite(UUID requestId,UUID target,CombatSource source,int ticks) implements EntityCommand {
        public Ignite { Objects.requireNonNull(requestId);Objects.requireNonNull(target);Objects.requireNonNull(source);if(ticks<0)throw new IllegalArgumentException("Negative fire duration"); }
    }
    record Displace(UUID requestId,UUID target,CombatSource source,Vector3 delta,Mode mode) implements EntityCommand {
        public enum Mode { POSITION, VELOCITY }
        public Displace { Objects.requireNonNull(requestId);Objects.requireNonNull(target);Objects.requireNonNull(source);Objects.requireNonNull(delta);Objects.requireNonNull(mode);if(!delta.finite())throw new IllegalArgumentException("Nonfinite displacement"); }
    }
}
