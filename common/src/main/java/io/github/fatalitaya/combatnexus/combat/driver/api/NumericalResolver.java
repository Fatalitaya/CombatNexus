package io.github.fatalitaya.combatnexus.combat.driver.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import java.util.List;
import java.util.UUID;

/** Synchronous external adjudication. Called in a read-only scope, never given write authority. */
public interface NumericalResolver {
    record Damage(UUID request, ParticipantIdentity target, CombatSource source, NexusId damageType, float amount) {}
    record Healing(HealingRequest request, ParticipantIdentity target) {}
    List<ResolvedDamage.Component> damage(Damage input);
    double healing(Healing input);
}
