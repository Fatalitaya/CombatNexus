package io.github.fatalitaya.combatnexus.combat.api.behavior;
import io.github.fatalitaya.combatnexus.config.DefinitionJson;
import java.util.*;
/** Installation-time typed values; no repository, entity or executable callback. */
public record BehaviorPlan(int priority, List<BehaviorComponent> components) {
    public static final BehaviorPlan NONE = new BehaviorPlan(0, List.of());
    public BehaviorPlan {
        DefinitionJson.range(priority, -10000, 10000, "priority");
        components = List.copyOf(components);
        if (components.size() > 64) throw new IllegalArgumentException("Behavior component budget exceeded");
        Set<io.github.fatalitaya.combatnexus.api.id.NexusId> types = new HashSet<>();
        for (var component : components) if (!types.add(component.type()))
            throw new IllegalArgumentException("Duplicate behavior component: " + component.type());
    }
    public <T extends BehaviorComponent> Optional<T> component(Class<T> type) {
        return components.stream().filter(type::isInstance).map(type::cast).findFirst();
    }
    public boolean empty() { return components.isEmpty(); }
    public Map<io.github.fatalitaya.combatnexus.api.id.NexusId, io.github.fatalitaya.combatnexus.api.id.NexusId> ownedShields(BehaviorSource source) {
        var result = new LinkedHashMap<io.github.fatalitaya.combatnexus.api.id.NexusId, io.github.fatalitaya.combatnexus.api.id.NexusId>();
        for (var component : components) component.ownedShields(source).forEach((id, pool) -> {
            if (result.putIfAbsent(id, pool) != null) throw new IllegalArgumentException("Duplicate resource identity: " + id);
        });
        return Map.copyOf(result);
    }
}
