package io.github.fatalitaya.combatnexus.api.entity;

import java.util.Locale;

/** Categories used by combat conditions and descriptions, independent of native spawning APIs. */
public enum EntityCategory {
    MONSTER, CREATURE, AMBIENT, AXOLOTLS, UNDERGROUND_WATER_CREATURE,
    WATER_CREATURE, WATER_AMBIENT, MISC;

    public String getName() { return name().toLowerCase(Locale.ROOT); }
}
