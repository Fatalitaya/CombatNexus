package io.github.fatalitaya.combatnexus.combat.action.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import io.github.fatalitaya.combatnexus.api.geometry.Vector3;
public sealed interface EntityCommand {
    UUID requestId(); UUID target(); CombatSource source();
    /** Vanilla knockback: x/z point toward the impact source, opposite to the push.
     * Resistance and native event adjustments are evaluated when this command executes. */
    record Knockback(UUID requestId, UUID target, CombatSource source,
                     double strength, double x, double z) implements EntityCommand {
        public Knockback {
            Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source);
            if (!Double.isFinite(strength) || strength < 0 || !Double.isFinite(x) || !Double.isFinite(z))
                throw new IllegalArgumentException("Invalid knockback");
        }
    }
    record Ignite(UUID requestId,UUID target,CombatSource source,int ticks) implements EntityCommand {
        public Ignite { Objects.requireNonNull(requestId);Objects.requireNonNull(target);Objects.requireNonNull(source);if(ticks<0)throw new IllegalArgumentException("Negative fire duration"); }
    }
    record Displace(UUID requestId,UUID target,CombatSource source,Vector3 delta,Mode mode,Origin origin) implements EntityCommand {
        public enum Origin { VOLUNTARY, EXTERNAL_CONTROL }
        public enum Mode { POSITION, VELOCITY }
        public Displace { Objects.requireNonNull(requestId);Objects.requireNonNull(target);Objects.requireNonNull(source);Objects.requireNonNull(delta);Objects.requireNonNull(mode);Objects.requireNonNull(origin);if(!delta.finite())throw new IllegalArgumentException("Nonfinite displacement"); }
    }
}
