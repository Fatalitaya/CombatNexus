package io.github.fatalitaya.combatnexus.combat.api.behavior;

import com.google.gson.JsonObject;
import io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit;
import io.github.fatalitaya.combatnexus.config.DefinitionJson;
import java.util.Objects;

/** Typed, immutable contributions. Each executor owns its own parameters and trigger limit. */
public interface BehaviorComponent {
    default boolean instant() { return false; }
    default io.github.fatalitaya.combatnexus.api.id.NexusId type() { return io.github.fatalitaya.combatnexus.combat.internal.registration.BehaviorTypes.type(this).id(); }
    default java.util.Map<io.github.fatalitaya.combatnexus.api.id.NexusId, io.github.fatalitaya.combatnexus.api.id.NexusId> ownedShields(BehaviorSource source) { return java.util.Map.of(); }
    default void revoke(BehaviorSource source, BehaviorCleanup cleanup) { ownedShields(source).values().forEach(cleanup::removeShield); }
    static BehaviorComponent decode(JsonObject json) { return io.github.fatalitaya.combatnexus.combat.internal.registration.BehaviorTypes.decode(json); }
}
