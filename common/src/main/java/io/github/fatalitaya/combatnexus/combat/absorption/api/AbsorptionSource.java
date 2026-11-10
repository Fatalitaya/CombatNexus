package io.github.fatalitaya.combatnexus.combat.absorption.api;

import io.github.fatalitaya.combatnexus.combat.api.CombatSource;

public record AbsorptionSource(CombatSource source, double capacity, double balance, double granted, double consumed) {}
