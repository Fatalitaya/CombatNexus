package io.github.fatalitaya.combatnexus.combat.action.api;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
public interface EntityCommands {
    CommandResult<Boolean> submit(EntityCommand command);
    CommandResult<Boolean> submit(EntityCommand command,CombatParent parent);
}
