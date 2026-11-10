package io.github.fatalitaya.combatnexus.combat.projectile.spi;

import io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch;
import java.util.Optional;

public interface NativeProjectile {
    Optional<ProjectileLaunch> combatnexus$launch();
    java.util.List<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample> combatnexus$savedSamples();
    void combatnexus$saveSamples(java.util.List<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample> samples);
    void combatnexus$captureLaunch(ProjectileLaunch launch);
}
