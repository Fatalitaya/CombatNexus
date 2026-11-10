package io.github.fatalitaya.combatnexus.combat.internal.persistence;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.effect.api.EffectInstance;
import io.github.fatalitaya.combatnexus.combat.internal.persistence.SavedBehavior;
import io.github.fatalitaya.combatnexus.combat.internal.persistence.SavedEquipment;
import io.github.fatalitaya.combatnexus.combat.shield.internal.SavedShield;
import io.github.fatalitaya.combatnexus.combat.regeneration.internal.SavedRecovery;

/** Detached data only. Runtime identities, tickets, and callbacks are never saved. */
public record CombatPersistentState(UUID target, long savedTick, Set<NexusId> persistentSources,
        List<EffectInstance> effects, List<SavedBehavior> behaviors, List<SavedShield> shields,
        List<SavedRecovery> recovery, List<SavedEquipment> equipment, List<io.github.fatalitaya.combatnexus.combat.internal.persistence.SavedSource> sources,
        List<io.github.fatalitaya.combatnexus.combat.driver.internal.SavedDriver> drivers,
        io.github.fatalitaya.combatnexus.combat.time.internal.SavedTime time,
        List<io.github.fatalitaya.combatnexus.combat.ability.internal.SavedAbility> abilities,
        List<io.github.fatalitaya.combatnexus.combat.ability.internal.SavedAbilityCooldown> abilityCooldowns,
        Map<UUID, io.github.fatalitaya.combatnexus.combat.api.source.DefinitionBindingSnapshot> definitionBindings) {
    public CombatPersistentState {
        definitionBindings = Map.copyOf(definitionBindings);
        if (definitionBindings.size() > 64) throw new IllegalArgumentException("Saved definition binding capacity");
        definitionBindings.forEach((id, binding) -> { if (!id.equals(binding.semanticIdentity())) throw new IllegalArgumentException("Definition binding identity mismatch"); });
        for (var source : sources) if (source.binding() != null && !definitionBindings.containsKey(source.binding()))
            throw new IllegalArgumentException("Missing source definition binding");
        if (savedTick < 0) throw new IllegalArgumentException("Invalid saved tick");
        Objects.requireNonNull(target); Objects.requireNonNull(time); sources = List.copyOf(sources); drivers = List.copyOf(drivers);
        abilities=List.copyOf(abilities);abilityCooldowns=List.copyOf(abilityCooldowns);
        if(abilities.size()>64||abilityCooldowns.size()>4096)throw new IllegalArgumentException("Saved ability capacity exceeded");
        if (drivers.size() > 64) throw new IllegalArgumentException("Driver ownership capacity exceeded");
        if (sources.size() > 64) throw new IllegalArgumentException("Source capacity exceeded");
        persistentSources = Set.copyOf(persistentSources); effects = List.copyOf(effects); behaviors = List.copyOf(behaviors);
        shields = List.copyOf(shields); recovery = List.copyOf(recovery); equipment = List.copyOf(equipment);
        if (persistentSources.size() > 64 || effects.size() > 256 || behaviors.size() > 64
                || shields.size() > 256 || recovery.size() > 256 || equipment.size() > 64)
            throw new IllegalArgumentException("Combat save capacity exceeded");
        if (effects.stream().anyMatch(e -> !e.snapshot().target().equals(target) || e.ownership() != io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED)
                || recovery.stream().anyMatch(e -> !e.plan().target().equals(target)))
            throw new IllegalArgumentException("Foreign or observed persistent state");
    }
}
