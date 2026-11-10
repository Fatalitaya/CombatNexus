package io.github.fatalitaya.combatnexus.combat.api;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationDefinition;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldDefinition;
import io.github.fatalitaya.combatnexus.effect.api.EffectDefinition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
/** Immutable domain definitions, adopted atomically with damage and behavior definitions. */
public record DomainDefinitions(Map<NexusId, HealingRuleDefinition> healingRules,
        Map<NexusId, RegenerationDefinition> regeneration, Map<NexusId, ShieldDefinition> shields,
        Map<NexusId, EffectDefinition> effects, Map<NexusId, io.github.fatalitaya.combatnexus.combat.ability.api.AbilityDefinition> abilities) {
    public static final DomainDefinitions EMPTY = new DomainDefinitions(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    public DomainDefinitions {
        healingRules = Map.copyOf(healingRules); regeneration = Map.copyOf(regeneration);
        shields = Map.copyOf(shields); effects = Map.copyOf(effects); abilities = Map.copyOf(abilities);
        abilities.forEach((id, definition) -> { if (!id.equals(definition.id())) throw new IllegalArgumentException("Ability identity mismatch"); });
        if (healingRules.size() > 64 || regeneration.size() > 256 || shields.size() > 256 || effects.size() > 256 || abilities.size() > 256)
            throw new IllegalArgumentException("Domain definition capacity exceeded");
    }
}
