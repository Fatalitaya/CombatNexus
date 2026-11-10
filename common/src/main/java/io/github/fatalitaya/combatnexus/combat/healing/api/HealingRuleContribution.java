package io.github.fatalitaya.combatnexus.combat.healing.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Already matched rule values, supplied in execution order. */
public record HealingRuleContribution(NexusId source, double additive, double multiplier) {}
