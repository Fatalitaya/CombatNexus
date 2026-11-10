package io.github.fatalitaya.combatnexus.combat.api.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal;
import java.util.List;
import java.util.Set;

/** Native death operations bound to an exact target and one resolved damage source. */
public interface LethalTarget extends HealthTarget {
    Set<NexusId> damageTags();
    boolean protect();
    List<ApplicationJournal.Entry> protectionEvidence();
    boolean removed();
    boolean alive();
    void deathSound();
    void rememberAttacker();
    void recordDamage(float amount);
    void die();
    boolean deathConfirmed();
}
