package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatAction;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceInstance;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import java.util.*;

/** Frozen release evidence. A successful release is explicitly confirmed by its executor. */
public record AbilityFact(Kind kind,ParticipantIdentity caster,NexusId ability,SourceInstance source,
        UUID execution,CombatAction action,long businessTick,AbilitySnapshot.Status result,boolean released) {
    public enum Kind { ADMITTED, RELEASED, TERMINAL }
    public AbilityFact { Objects.requireNonNull(kind);Objects.requireNonNull(caster);Objects.requireNonNull(ability);Objects.requireNonNull(source);Objects.requireNonNull(execution);Objects.requireNonNull(action);Objects.requireNonNull(result);if(businessTick<0)throw new IllegalArgumentException("Negative business time"); }
}
