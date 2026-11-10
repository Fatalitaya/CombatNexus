package io.github.fatalitaya.combatnexus.presentation.api;

import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldPoolSnapshot;
import io.github.fatalitaya.combatnexus.effect.api.EffectView;
import io.github.fatalitaya.combatnexus.formula.Amounts;
import java.util.List;
import java.util.Objects;

/** Read-only server model used by authenticated panel projection. */
public record CombatPanelSnapshot(AttributeSnapshot attributes, double health, double vanillaAbsorption,
                                   List<ShieldPoolSnapshot> shields, List<EffectView> effects,
                                   List<BehaviorObservation> behaviors,
                                   List<io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionFact> reactions,
                                   List<io.github.fatalitaya.combatnexus.combat.api.source.SourceDescription> sourceDescriptions,
                                   List<io.github.fatalitaya.combatnexus.combat.api.source.SourceView> sources, List<EquipmentView> equipment) {
    public record BehaviorObservation(java.util.UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId effect,
            io.github.fatalitaya.combatnexus.api.id.NexusId source, long installation, boolean protectionAvailable,
            List<io.github.fatalitaya.combatnexus.api.id.NexusId> componentTypes) {
        public BehaviorObservation { Objects.requireNonNull(target); Objects.requireNonNull(effect); Objects.requireNonNull(source); componentTypes = List.copyOf(componentTypes); }
    }
    public record EquipmentView(java.util.UUID installation,io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition slot,io.github.fatalitaya.combatnexus.api.id.NexusId binding,io.github.fatalitaya.combatnexus.api.id.NexusId definition,boolean selected) {}
    public CombatPanelSnapshot(AttributeSnapshot attributes, double health, double vanillaAbsorption,
            List<ShieldPoolSnapshot> shields, List<EffectView> effects) {
        this(attributes, health, vanillaAbsorption, shields, effects, List.of(), List.of(), List.of(), List.of(), List.of());
    }
    public CombatPanelSnapshot {
        sources=List.copyOf(sources);equipment=List.copyOf(equipment);
        behaviors = List.copyOf(behaviors); reactions = List.copyOf(reactions);
        sourceDescriptions=List.copyOf(sourceDescriptions);
        if (behaviors.stream().anyMatch(b -> !b.target().equals(attributes.entity()))
                || reactions.stream().anyMatch(f -> !f.target().equals(attributes.entity()))) throw new IllegalArgumentException("Foreign panel behavior");
        Objects.requireNonNull(attributes, "attributes");
        Amounts.nonNegative(health, "health");
        Amounts.nonNegative(vanillaAbsorption, "vanillaAbsorption");
        shields = List.copyOf(shields);
        effects = List.copyOf(effects);
        if (effects.stream().anyMatch(effect -> !effect.target().equals(attributes.entity()))) {
            throw new IllegalArgumentException("Panel effect target mismatch");
        }
    }
}
