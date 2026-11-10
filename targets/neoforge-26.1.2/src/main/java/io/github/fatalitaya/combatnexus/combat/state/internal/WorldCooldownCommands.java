package io.github.fatalitaya.combatnexus.combat.state.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal;
import io.github.fatalitaya.combatnexus.combat.api.CommandRejection;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.StateCommands;
import io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommand;
import io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommands;
import io.github.fatalitaya.combatnexus.combat.state.api.CooldownStamp;
import io.github.fatalitaya.combatnexus.combat.state.spi.NativeCooldownTarget;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** World-owned domain commands; mutations enter through StateCommands. */
public final class WorldCooldownCommands implements CooldownCommands {
    private final CombatRuntime world;
    public WorldCooldownCommands(CombatRuntime world) { this.world = Objects.requireNonNull(world); }

    public Optional<CooldownStamp> snapshot(UUID target, NexusId source) {
        var entity=world.level().getEntity(target);
        return entity instanceof NativeCooldownTarget nativeTarget
                ? Optional.ofNullable(nativeTarget.combatnexus$cooldowns().get(source)) : Optional.empty();
    }
    public long remaining(UUID target, NexusId source, int duration) {
        if(duration<0) throw new IllegalArgumentException("Negative cooldown duration");
        return snapshot(target, source).map(stamp -> stamp.remaining(world.businessTick(target, source), duration)).orElse(0L);
    }
    public CommandResult<Boolean> submit(CooldownCommand command) { return submit(command, null); }
    public CommandResult<Boolean> submit(CooldownCommand command, CombatParent parent) {
        return StateCommands.submit(world, command.requestId(), command.target(), CombatOperation.STATE, Optional.of(command.source()), parent, () -> {
            var entries=((NativeCooldownTarget)world.level().getEntity(command.target())).combatnexus$cooldowns();
            var previous=entries.get(command.source().id());
            if(command.operation()==CooldownCommand.Operation.START) {
                if(previous==null && entries.size()>=64) return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Cooldown source capacity");
                entries.put(command.source().id(),new CooldownStamp(world.businessTick(command.target(),command.source().id()),command.deathPolicy()));
            } else entries.remove(command.source().id());
            return CommandResult.applied(true).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(
                    ApplicationJournal.Resource.COOLDOWN,command.source().id(),previous==null?0:1,entries.containsKey(command.source().id())?1:0,1,true)),true));
        });
    }

}
