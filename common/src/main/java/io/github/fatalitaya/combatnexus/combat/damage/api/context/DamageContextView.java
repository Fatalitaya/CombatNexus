package io.github.fatalitaya.combatnexus.combat.damage.api.context;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecisionSnapshot;
import java.util.*;

/** Rule-facing calculation view. All participant observations are frozen before callbacks. */
public interface DamageContextView {
    float defenseInput(DefenseInput input);
    float offensiveAmount();
    NativeDamageInput nativeInput();
    List<DamageChannel> activeChannels();
    Optional<AttributeSnapshot> attributeSnapshot(UUID entity);
    DamageChannel resolveChannel(NexusId id);
    DamageParticipantFacts targetFacts();
    Optional<DamageParticipantFacts> attackerFacts();
    Optional<NexusId> damageType();
    Set<NexusId> damageTypeTags();
    DamageTriggerPolicy triggerPolicy();
    boolean hasParentDamage();
    boolean isManaged();
    DamageRequestKind requestKind();
    Optional<NexusId> actionId();
    Set<NexusId> sourceTags();
    DamageMetadata metadata();
    DamageAttributionProvenance attributionProvenance();
    long damageId();
    DamageChannel getInitialChannel();
    boolean hasActiveDamageInChannel(DamageChannel channel);
    DamagePhase currentPhase();
    boolean isCritical();
    CriticalDecisionSnapshot criticalDecision();
    boolean isDamageCancelled();
}
