package io.github.fatalitaya.combatnexus.combat.state.internal;

import io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal;
import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.StateCommands;
import io.github.fatalitaya.combatnexus.combat.state.api.RuntimeStates;
import io.github.fatalitaya.combatnexus.combat.state.api.StateAddress;
import io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** World-owned domain commands; mutations enter through StateCommands. */
public final class WorldStateCommands implements RuntimeStates {
    private final CombatRuntime world;
    public WorldStateCommands(CombatRuntime world) { this.world = Objects.requireNonNull(world); }

    public <T> Optional<T> read(StateAddress<T> address) {
        var entity=world.level().getEntity(address.target());return entity instanceof NativeRuntimeStates state?state.combatnexus$states().read(address):Optional.empty();
    }
    public <T> CommandResult<T> write(UUID request,StateAddress<T> address,CombatSource source,T value) { return write(request,address,source,value,null); }
    public <T> CommandResult<T> write(UUID request,StateAddress<T> address,CombatSource source,T value,CombatParent parent) {
        return StateCommands.submit(world,request,address.target(),CombatOperation.STATE,Optional.of(source),parent,()->{
            var state=((NativeRuntimeStates)world.level().getEntity(address.target())).combatnexus$states();
            boolean existed=state.read(address).isPresent();var detached=state.write(address,value);
            return CommandResult.applied(detached).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.STATE,address.key().id(),existed?1:0,1,1,true)),true));
        });
    }
    public CommandResult<Boolean> remove(UUID request,StateAddress<?> address,CombatSource source) { return remove(request,address,source,null); }
    public CommandResult<Boolean> remove(UUID request,StateAddress<?> address,CombatSource source,CombatParent parent) {
        return StateCommands.submit(world,request,address.target(),CombatOperation.STATE,Optional.of(source),parent,()->{
            boolean removed=((NativeRuntimeStates)world.level().getEntity(address.target())).combatnexus$states().remove(address);
            return CommandResult.applied(removed).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.STATE,address.key().id(),removed?1:0,0,removed?1:0,true)),true));
        });
    }
    public <T> CommandResult<Optional<T>> take(UUID request,StateAddress<T> address,CombatSource source) { return take(request,address,source,null); }
    public <T> CommandResult<Optional<T>> take(UUID request,StateAddress<T> address,CombatSource source,CombatParent parent) {
        return StateCommands.submit(world,request,address.target(),CombatOperation.STATE,Optional.of(source),parent,()->{
            var taken=((NativeRuntimeStates)world.level().getEntity(address.target())).combatnexus$states().take(address);
            return CommandResult.applied(taken).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.STATE,address.key().id(),taken.isPresent()?1:0,0,taken.isPresent()?1:0,true)),true));
        });
    }

}
