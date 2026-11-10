package io.github.fatalitaya.combatnexus.combat.api.platform;

/** Exact entity identity shared across nested native health writes. */
public interface HealthTarget {
    float health();
    float maximumHealth();
    void health(float value);
}
