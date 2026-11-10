package io.github.fatalitaya.combatnexus.combat.state.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
public interface RuntimeStates {
    <T> Optional<T> read(StateAddress<T> address);
    <T> CommandResult<T> write(UUID request,StateAddress<T> address,CombatSource source,T value);
    <T> CommandResult<T> write(UUID request,StateAddress<T> address,CombatSource source,T value,CombatParent parent);
    CommandResult<Boolean> remove(UUID request,StateAddress<?> address,CombatSource source);
    CommandResult<Boolean> remove(UUID request,StateAddress<?> address,CombatSource source,CombatParent parent);
    <T> CommandResult<Optional<T>> take(UUID request,StateAddress<T> address,CombatSource source);
    <T> CommandResult<Optional<T>> take(UUID request,StateAddress<T> address,CombatSource source,CombatParent parent);
}
