package io.github.fatalitaya.combatnexus.combat.state.api;
import java.util.*;
import com.mojang.serialization.Codec;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** A startup definition explicitly owns state lifetime; queries never advance or clear it.
 * The copy strategy must return an independently owned value; immutable types may explicitly use
 * UnaryOperator.identity(). The codec validates writes and handles persistence, not in-memory reads.
 */
public record StateKey<T>(NexusId id,Codec<T> codec,java.util.function.UnaryOperator<T> copy,Scope scope,Set<Boundary> clearOn,boolean persistent) {
    public enum Scope { ENTITY, SOURCE_INSTALLATION, ACTION }
    public enum Boundary { DEATH, RESPAWN, SOURCE_REMOVED, DIMENSION_CHANGE, ACTION_FINISHED }
    public StateKey { Objects.requireNonNull(id);Objects.requireNonNull(codec);Objects.requireNonNull(copy);Objects.requireNonNull(scope);clearOn=Set.copyOf(clearOn);if(scope==Scope.ACTION&&persistent)throw new IllegalArgumentException("Action state is session scoped"); }
}
