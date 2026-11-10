package io.github.fatalitaya.combatnexus.combat.api.behavior;
/** Server-computed display state; clients never infer remaining protection from elapsed time. */
public record BehaviorView(BehaviorSource source, boolean protectionAvailable) {}
