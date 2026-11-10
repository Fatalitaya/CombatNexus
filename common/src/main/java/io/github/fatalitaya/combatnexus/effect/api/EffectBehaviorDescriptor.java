package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorComponent;
import io.github.fatalitaya.combatnexus.presentation.api.TooltipBlock;

/** Startup-registered semantic explanation, evaluated on the server with the adopted level. */
@FunctionalInterface
public interface EffectBehaviorDescriptor<T extends BehaviorComponent> {
    TooltipBlock describe(T behavior, int level);
}
