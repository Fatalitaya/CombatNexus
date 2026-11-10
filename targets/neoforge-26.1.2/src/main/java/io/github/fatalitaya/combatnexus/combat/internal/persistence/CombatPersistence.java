package io.github.fatalitaya.combatnexus.combat.internal.persistence;

import java.util.UUID;

/** Coordinates domain persistence without owning domain state. */
public final class CombatPersistence {
    private final io.github.fatalitaya.combatnexus.effect.internal.EffectServiceControl effects;
    private final io.github.fatalitaya.combatnexus.combat.shield.internal.ShieldPoolServiceControl shields;
    private final io.github.fatalitaya.combatnexus.combat.regeneration.internal.RegenerationSchedulerControl regeneration;
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.WorldBehaviorSources behaviors;
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.EquipmentBehaviorController equipment;
    private final io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations sources;
    private final java.util.Map<UUID, java.util.Set<io.github.fatalitaya.combatnexus.api.id.NexusId>> persistentSources;
    private final io.github.fatalitaya.combatnexus.combat.time.internal.WorldBusinessTime time;
    private final io.github.fatalitaya.combatnexus.combat.driver.internal.WorldDrivers drivers;
    private final io.github.fatalitaya.combatnexus.combat.ability.internal.DefaultAbilityService abilities;
    private final java.util.function.LongSupplier gameClock;
    public CombatPersistence(io.github.fatalitaya.combatnexus.effect.internal.EffectServiceControl effects, io.github.fatalitaya.combatnexus.combat.shield.internal.ShieldPoolServiceControl shields, io.github.fatalitaya.combatnexus.combat.regeneration.internal.RegenerationSchedulerControl regeneration, io.github.fatalitaya.combatnexus.combat.internal.coordination.WorldBehaviorSources behaviors, io.github.fatalitaya.combatnexus.combat.internal.coordination.EquipmentBehaviorController equipment, io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations sources, java.util.Map<UUID, java.util.Set<io.github.fatalitaya.combatnexus.api.id.NexusId>> persistentSources, java.util.function.LongSupplier gameClock, io.github.fatalitaya.combatnexus.combat.time.internal.WorldBusinessTime time, io.github.fatalitaya.combatnexus.combat.driver.internal.WorldDrivers drivers, io.github.fatalitaya.combatnexus.combat.ability.internal.DefaultAbilityService abilities) {
        this.abilities=abilities;
        this.time=time; this.drivers=drivers;
        this.effects = effects;
        this.shields = shields;
        this.regeneration = regeneration;
        this.behaviors = behaviors;
        this.equipment = equipment;
        this.sources = sources;
        this.persistentSources = persistentSources;
        this.gameClock = gameClock;
    }
    public String save(UUID target) {
        var explicit = persistentSources.getOrDefault(target, java.util.Set.of());
        var sources = new java.util.HashSet<>(explicit); sources.addAll(equipment.sources(target)); sources.addAll(this.sources.persistentKeys(target));
        var retained = new java.util.HashSet<>(explicit);
        retained.addAll(effects.persistentSources(target));
        sources.addAll(retained);
        var shieldSources = new java.util.HashSet<>(sources); shieldSources.addAll(behaviors.persistentPools(target, sources));
        var state = new io.github.fatalitaya.combatnexus.combat.internal.persistence.CombatPersistentState(target, gameClock.getAsLong(), retained,
                effects.save(target, sources),
                behaviors.save(target, sources, gameClock.getAsLong()),
                shields.save(target, shieldSources),
                regeneration.save(target, sources, gameClock.getAsLong()), equipment.save(target), this.sources.save(target), drivers.save(target), time.save(target), abilities.save(target,sources), abilities.saveCooldowns(target), this.sources.saveBindings(target));
        return io.github.fatalitaya.combatnexus.combat.internal.persistence.CombatStateCodec.encode(state);
    }
    public void restore(UUID target, String encoded) {
        var state = io.github.fatalitaya.combatnexus.combat.internal.persistence.CombatStateCodec.decode(encoded);
        if (!state.target().equals(target)) throw new IllegalArgumentException("Persistent target mismatch");
        effects.restoring(true);
        try {
        time.restore(target,state.time());
        drivers.restore(target,state.drivers());
        long now = gameClock.getAsLong();
        var acceptedEquipment = equipment.restore(target, state.equipment());
        this.sources.restore(target, state.sources().stream().filter(s -> !s.source().provider().equals(io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("combatnexus", "equipment"))
            || acceptedEquipment.contains(io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations.key(s.source()))).toList(), state.definitionBindings());
        var sources = new java.util.HashSet<>(state.persistentSources()); sources.addAll(acceptedEquipment); sources.addAll(this.sources.persistentKeys(target));
        persistentSources.put(target, new java.util.HashSet<>(state.persistentSources()));
        behaviors.restoring(true);
        try {
        this.sources.restoreSuspensions(target);
        effects.restore(target, state.effects(), sources);
        behaviors.restore(target, state.behaviors(), now);
        var pools = behaviors.restoredPools(target, state.behaviors());
        shields.restore(target, state.shields(), sources, pools);
        regeneration.restore(target, state.recovery(), sources);
        this.sources.restoreSuspensions(target);
        }finally{behaviors.restoring(false);}
        equipment.restored(target);
        this.sources.notifyRestored(target);
        abilities.restore(target,state.abilities(),state.abilityCooldowns());
        }finally{effects.restoring(false);}
    }
}
