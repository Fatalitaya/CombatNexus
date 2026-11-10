package io.github.fatalitaya.combatnexus.combat.api.behavior;
import com.google.gson.JsonObject;
import io.github.fatalitaya.combatnexus.config.DefinitionJson;
import io.github.fatalitaya.combatnexus.config.SettingsJson;
import java.util.*;
import io.github.fatalitaya.combatnexus.effect.api.EffectLifetime;
/** Definitions remain inert until explicitly installed. */
public record BehaviorDefinition(BehaviorPlan plan, EffectLifetime lifetime) {
    public BehaviorDefinition {
        Objects.requireNonNull(plan); Objects.requireNonNull(lifetime);
        if (lifetime.kind() == EffectLifetime.Kind.TIMED) DefinitionJson.range(lifetime.ticks(), 1, 1200000, "lifetime.ticks");
    }
    public static BehaviorDefinition decode(JsonObject json) {
        SettingsJson.fields(json, "priority", "lifetime", "components");
        List<BehaviorComponent> components = new ArrayList<>();
        for (var value : DefinitionJson.array(json, "components", 64)) components.add(BehaviorComponent.decode(value.getAsJsonObject()));
        return new BehaviorDefinition(new BehaviorPlan(SettingsJson.integer(json, "priority"), components), EffectLifetime.decode(json.getAsJsonObject("lifetime")));
    }
}
