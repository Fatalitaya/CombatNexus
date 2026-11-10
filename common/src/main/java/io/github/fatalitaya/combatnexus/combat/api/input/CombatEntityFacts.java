package io.github.fatalitaya.combatnexus.combat.api.input;

import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import java.util.*;

/** Request-scoped input captured before extension callbacks; mutable values never escape. */
public final class CombatEntityFacts {
    private final io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts entityType;
    public io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts entityType() { return entityType; }
    private final io.github.fatalitaya.combatnexus.api.entity.EntityCategory category;
    public io.github.fatalitaya.combatnexus.api.entity.EntityCategory category() { return category; }
    private final float health, maxHealth;
    private final boolean onFire;
    private final FrozenFacts external;
    private final ItemFacts weapon;
    private final int chargeTicks;
    private final float attackStrength;
    private final Map<EquipmentPosition, ItemFacts> equipment = new EnumMap<>(EquipmentPosition.class);
    private final io.github.fatalitaya.combatnexus.effect.api.EffectFacts effects;
    public CombatEntityFacts(io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts entityType, io.github.fatalitaya.combatnexus.api.entity.EntityCategory category,
            float health, float maxHealth, boolean onFire, FrozenFacts external, ItemFacts weapon,
            int chargeTicks, float attackStrength, Map<EquipmentPosition, ItemFacts> equipment,
            io.github.fatalitaya.combatnexus.effect.api.EffectFacts effects) {
        this.category = Objects.requireNonNull(category); this.entityType = Objects.requireNonNull(entityType); this.health = health; this.maxHealth = maxHealth;
        this.onFire = onFire; this.external = Objects.requireNonNull(external); this.weapon = Objects.requireNonNull(weapon);
        this.chargeTicks = chargeTicks; this.attackStrength = attackStrength; this.effects = Objects.requireNonNull(effects);
        if (!equipment.keySet().equals(Set.of(EquipmentPosition.values()))) throw new IllegalArgumentException("Incomplete frozen equipment");
        this.equipment.putAll(equipment);
    }
    public io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageParticipantFacts ruleFacts() {
        return new io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageParticipantFacts(entityType, category, health, maxHealth, onFire,
            effects.views().stream().map(io.github.fatalitaya.combatnexus.effect.api.EffectView::definition).collect(java.util.stream.Collectors.toUnmodifiableSet()),
            effects.values().stream().map(io.github.fatalitaya.combatnexus.effect.api.EffectFacts.Observed::id).collect(java.util.stream.Collectors.toUnmodifiableSet()),
            effects.values().stream().flatMap(value -> value.tags().stream()).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
    public Map<EquipmentPosition, ItemFacts> equipment() { return Map.copyOf(equipment); }
    public io.github.fatalitaya.combatnexus.effect.api.EffectFacts effectFacts() { return effects; }
    public float health() { return health; }
    public float maxHealth() { return maxHealth; }
    public boolean onFire() { return onFire; }
    public FrozenFacts external() { return external; }
    public ItemFacts weapon(){return weapon;}
    public int chargeTicks(){return chargeTicks;}
    public float attackStrength(){return attackStrength;}
    public ItemFacts equipment(EquipmentPosition slot) { return equipment.get(Objects.requireNonNull(slot)); }
    public List<io.github.fatalitaya.combatnexus.effect.api.EffectFacts.Observed> effects() { return effects.values(); }
    public List<io.github.fatalitaya.combatnexus.effect.api.EffectView> effectViews() { return effects.views(); }
    public boolean hasEffect(io.github.fatalitaya.combatnexus.api.id.NexusId effect) { return effects.hasEffect(effect); }
    public boolean hasNativeEffect(io.github.fatalitaya.combatnexus.api.id.NexusId effect) { return effects.hasNativeEffect(effect); }
    public OptionalInt amplifier(io.github.fatalitaya.combatnexus.api.id.NexusId effect) {
        return effects.amplifier(effect);
    }
}
