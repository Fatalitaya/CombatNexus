package io.github.fatalitaya.combatnexus.combat.api.platform;

import io.github.fatalitaya.combatnexus.api.geometry.Vector3;
import io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand.Displace.Mode;
import java.util.UUID;

/** Bound to one world at load; operations address an already validated living target. */
public interface EntityMotionAccess {
    int fireTicks(UUID target);
    void fireTicks(UUID target, int ticks);
    Vector3 vector(UUID target, Mode mode);
    void vector(UUID target, Mode mode, Vector3 value);
}
