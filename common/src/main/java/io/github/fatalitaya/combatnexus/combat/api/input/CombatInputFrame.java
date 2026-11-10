package io.github.fatalitaya.combatnexus.combat.api.input;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;

/** One participant's frozen input. Identity is descriptive and cannot grant execution authority. */
public record CombatInputFrame(UUID sessionId, NexusId dimension, UUID entity, long revision,
        long gameTick, long definitionGeneration, io.github.fatalitaya.combatnexus.attribute.api.AttributeValueFrame attributes, io.github.fatalitaya.combatnexus.attribute.api.AttributeProvenance provenance, CombatEntityFacts facts, java.util.List<io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource> behaviors) {
    public AttributeSnapshot attributeSnapshot() { return new AttributeSnapshot(attributes, provenance); }
    public CombatInputFrame {
        behaviors = java.util.List.copyOf(behaviors);
        Objects.requireNonNull(sessionId); Objects.requireNonNull(dimension); Objects.requireNonNull(entity);
        Objects.requireNonNull(provenance); Objects.requireNonNull(attributes);
        if (!provenance.entity().equals(entity) || provenance.revision() != revision || provenance.gameTick() != gameTick || !attributes.values().keySet().equals(provenance.sources().keySet())) throw new IllegalArgumentException("Provenance capture mismatch");
        Objects.requireNonNull(facts);
        if (behaviors.size() > 64 || behaviors.stream().anyMatch(s -> !s.target().equals(entity) || !s.session().equals(sessionId)))
            throw new IllegalArgumentException("Behavior capture identity or capacity mismatch");
        if (!entity.equals(attributes.entity()) || revision != attributes.revision() || gameTick != attributes.gameTick())
            throw new IllegalArgumentException("Input capture identity mismatch");
    }
}
