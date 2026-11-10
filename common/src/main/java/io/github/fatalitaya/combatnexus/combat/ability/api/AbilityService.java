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
    AbilityResult reinput(AbilityReinput request);
    AbilityResult reinput(AbilityReinput request, CombatParent parent);
    CommandResult<Boolean> cancel(UUID request, UUID actor, UUID execution);
    AbilityCooldownSnapshot cooldown(UUID actor, AbilityCooldownSlot slot);
    CommandResult<AbilityCooldownSnapshot.Change> cooldown(AbilityCooldownCommand command);
    CommandResult<AbilityCooldownSnapshot.Change> cooldown(AbilityCooldownCommand command, CombatParent parent);
    Optional<AbilitySnapshot> execution(UUID execution);
}
