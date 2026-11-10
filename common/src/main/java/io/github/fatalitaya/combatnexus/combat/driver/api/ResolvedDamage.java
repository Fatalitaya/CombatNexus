package io.github.fatalitaya.combatnexus.combat.driver.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import java.util.*;

/** Already resolved channel components, before the application mechanisms owned by CombatNexus. */
public record ResolvedDamage(UUID requestId, CombatSource source, NexusId damageType, List<Component> components) {
    public record Component(NexusId channel, float amount) {
        public Component { Objects.requireNonNull(channel); if (!Float.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid resolved component"); }
    }
    public ResolvedDamage {
        Objects.requireNonNull(requestId); Objects.requireNonNull(source); Objects.requireNonNull(damageType);
        components = List.copyOf(components);
        if (components.isEmpty() || components.size() > 32) throw new IllegalArgumentException("Resolved component budget");
        float total = 0; for (var component : components) total += component.amount();
        if (!Float.isFinite(total) || total > 1000000) throw new IllegalArgumentException("Resolved damage amount exceeds budget");
    }
    public float amount() { float total = 0; for (var component : components) total += component.amount(); return total; }
    public Map<NexusId, Float> channels() {
        var result = new LinkedHashMap<NexusId, Float>();
        components.forEach(c -> result.merge(c.channel(), c.amount(), Float::sum));
        return Collections.unmodifiableMap(result);
    }
}
