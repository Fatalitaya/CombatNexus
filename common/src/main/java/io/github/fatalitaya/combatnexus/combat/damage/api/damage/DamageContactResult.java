package io.github.fatalitaya.combatnexus.combat.damage.api.damage;
/** Contact outcome is independent of health, shield, absorption and equipment loss.
 * REJECTED includes native refusal whose more precise cause was not observed. */
public enum DamageContactResult { ACCEPTED, BLOCKED, INTERCEPTED, IMMUNE, CANCELLED, REJECTED, UNKNOWN }
