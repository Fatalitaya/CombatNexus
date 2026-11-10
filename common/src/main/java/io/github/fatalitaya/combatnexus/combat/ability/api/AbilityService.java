package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
import java.util.*;

public interface AbilityService {
    AbilityDefinition definition(AbilityGrant grant);
    Map<NexusId,AbilityDefinition> definitions();
    List<AbilityGrant> grants(UUID actor);
    AbilityAvailability evaluate(AbilityRequest request);
    AbilityResult execute(AbilityRequest request);
    AbilityResult execute(AbilityRequest request, CombatParent parent);
    CommandResult<Boolean> cancel(UUID request, UUID actor, UUID execution);
    Optional<AbilitySnapshot> execution(UUID execution);
}
