package io.github.fatalitaya.combatnexus.combat.ability.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.ability.api.*;
import io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope;
import java.util.*;

/** A waiting continuation checkpoint. Completed or uncertain steps are never restored as pending. */
public record SavedAbility(UUID execution,UUID action,UUID source,NexusId contribution,AbilityDefinition definition,
                           AbilityTarget target,AbilityParameters input,AbilityParameters state,int step,
                           DriverScope scope,long remaining,List<AbilitySnapshot.CommandEvidence> commands,List<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample> samples,boolean released) {
    public SavedAbility {
        Objects.requireNonNull(execution);Objects.requireNonNull(action);Objects.requireNonNull(source);Objects.requireNonNull(contribution);
        Objects.requireNonNull(definition);Objects.requireNonNull(target);Objects.requireNonNull(input);Objects.requireNonNull(state);Objects.requireNonNull(scope);commands=List.copyOf(commands);samples=List.copyOf(samples);
        if(samples.size()>256||step<0||step>62||remaining<0||remaining>1200000||commands.size()>256||commands.stream().anyMatch(c->!c.journal().applicationCompleted()))throw new IllegalArgumentException("Invalid ability checkpoint");
    }
}
