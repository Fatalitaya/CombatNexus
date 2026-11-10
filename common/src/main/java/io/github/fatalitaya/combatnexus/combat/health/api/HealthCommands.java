package io.github.fatalitaya.combatnexus.combat.health.api;

import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;

public interface HealthCommands {
    CommandResult<Float> correct(HealthCorrection command);
    CommandResult<Float> correct(HealthCorrection command, CombatParent parent);
    CommandResult<LethalReceipt> execute(LethalAttempt command);
    CommandResult<LethalReceipt> execute(LethalAttempt command, CombatParent parent);
}
