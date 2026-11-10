package io.github.fatalitaya.combatnexus.combat.state.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
public interface RuntimeStates {
    <T> Optional<T> read(StateAddress<T> address);
    <T> CommandResult<T> write(UUID request,StateAddress<T> address,CombatSource source,T value);
    CommandResult<Boolean> remove(UUID request,StateAddress<?> address,CombatSource source);
}
