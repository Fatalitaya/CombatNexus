package io.github.fatalitaya.combatnexus.combat.action.internal;

import io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand;
import io.github.fatalitaya.combatnexus.combat.action.api.EntityCommands;
import io.github.fatalitaya.combatnexus.combat.api.CommandRejection;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.StateCommands;
import io.github.fatalitaya.combatnexus.effect.api.ControlCategory;
import io.github.fatalitaya.combatnexus.effect.api.EffectSnapshot;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** World-owned domain commands; mutations enter through StateCommands. */
public final class WorldEntityCommands implements EntityCommands {
    private final CombatRuntime world;
    public WorldEntityCommands(CombatRuntime world) { this.world = Objects.requireNonNull(world); }

    public CommandResult<Boolean> submit(EntityCommand command) { return submit(command,null); }
    public CommandResult<Boolean> submit(EntityCommand command,CombatParent parent) {
        return StateCommands.submit(world,command.requestId(),command.target(),command instanceof EntityCommand.Ignite?CombatOperation.IGNITION:CombatOperation.MOVEMENT,
                Optional.of(command.source()),parent,null,command instanceof EntityCommand.Knockback,()->{
                    boolean external=command instanceof EntityCommand.Knockback
                        ||command instanceof EntityCommand.Displace displacement&&displacement.origin()==EntityCommand.Displace.Origin.EXTERNAL_CONTROL;
                    if(command instanceof EntityCommand.Displace){
                        var restriction=external?EffectSnapshot.RestrictedAction.EXTERNAL_MOVEMENT:EffectSnapshot.RestrictedAction.SKILL_MOVEMENT;
                        if((world.effects().restrictionMask(command.target())&(1<<restriction.ordinal()))!=0)return CommandResult.rejected(CommandRejection.Code.IMMUNE,"Movement action restricted");
                    }
                    if(external&&world.effects().immune(command.target(),command.source().id(),Set.of(),Set.of(ControlCategory.MOVEMENT)))return CommandResult.rejected(CommandRejection.Code.IMMUNE,"Movement control immunity");
                    return EntityApplication.apply(world.motion(),command);
                });
    }

}
