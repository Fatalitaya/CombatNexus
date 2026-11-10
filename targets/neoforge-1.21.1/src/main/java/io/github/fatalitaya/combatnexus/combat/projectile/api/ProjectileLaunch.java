package io.github.fatalitaya.combatnexus.combat.projectile.api;

import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame;

/** Captured once; later owner/equipment changes cannot replace the launching participant. */
public record ProjectileLaunch(UUID action, UUID projectile, io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture.Participant shooterCapture,io.github.fatalitaya.combatnexus.combat.api.input.CapturedItem weapon) {
    public CombatInputFrame shooter() { return shooterCapture.frame(); }
    public ProjectileLaunch { Objects.requireNonNull(action); Objects.requireNonNull(projectile); Objects.requireNonNull(shooterCapture);Objects.requireNonNull(weapon); }
}
