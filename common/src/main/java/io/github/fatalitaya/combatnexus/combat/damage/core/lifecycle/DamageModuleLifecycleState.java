package io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle;

public enum DamageModuleLifecycleState {
    CONSTRUCTING,
    REGISTERING,
    FROZEN,
    RUNNING,

    /**
     * Terminal state entered when framework bootstrap fails after
     * registration has begun.
     */
    FAILED
}
