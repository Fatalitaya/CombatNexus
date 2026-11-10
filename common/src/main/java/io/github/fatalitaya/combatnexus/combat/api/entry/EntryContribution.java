package io.github.fatalitaya.combatnexus.combat.api.entry;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.*;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter;

/** A locally identified contribution; execution remains owned by its domain. */
public sealed interface EntryContribution {
    NexusId id();
    enum Kind { DAMAGE, ATTRIBUTE, BEHAVIOR, EFFECT, SHIELD, REGENERATION, ABILITY }
    Kind kind();
    record Damage(NexusId id, DamageRuleDefinition rule, DamageRuleSourceFilter source) implements EntryContribution {
        public Damage { Objects.requireNonNull(id); Objects.requireNonNull(rule); Objects.requireNonNull(source); }
        public Kind kind() { return Kind.DAMAGE; }
    }
    record Attribute(NexusId id, NexusId attribute, double amount, AttributeOperation operation) implements EntryContribution {
        public Attribute { Objects.requireNonNull(id); Objects.requireNonNull(attribute); Objects.requireNonNull(operation);
            if (!Double.isFinite(amount) || Math.abs(amount)>1000000) throw new IllegalArgumentException("Invalid attribute amount"); }
        public Kind kind() { return Kind.ATTRIBUTE; }
    }
    record Behavior(NexusId id, NexusId definition) implements EntryContribution {
        public Behavior { Objects.requireNonNull(id); Objects.requireNonNull(definition); }
        public Kind kind() { return Kind.BEHAVIOR; }
    }
    record Effect(NexusId id, NexusId definition) implements EntryContribution {
        public Effect { Objects.requireNonNull(id); Objects.requireNonNull(definition); }
        public Kind kind() { return Kind.EFFECT; }
    }
    record Shield(NexusId id, NexusId definition, double amount, int durationTicks) implements EntryContribution {
        public Shield { Objects.requireNonNull(id); Objects.requireNonNull(definition);
            if (!Double.isFinite(amount)||amount<0||durationTicks<1||durationTicks>1200000) throw new IllegalArgumentException("Invalid shield grant"); }
        public Kind kind() { return Kind.SHIELD; }
    }
    record Regeneration(NexusId id, NexusId definition) implements EntryContribution {
        public Regeneration { Objects.requireNonNull(id); Objects.requireNonNull(definition); }
        public Kind kind() { return Kind.REGENERATION; }
    }
    record Ability(NexusId id, NexusId definition) implements EntryContribution {
        public Ability { Objects.requireNonNull(id); Objects.requireNonNull(definition); }
        public Kind kind() { return Kind.ABILITY; }
    }
    MapCodec<Damage> DAMAGE = RecordCodecBuilder.mapCodec(i->i.group(
        NexusIdCodecs.CODEC.fieldOf("id").forGetter(Damage::id),DamageRuleDefinition.CODEC.fieldOf("rule").forGetter(Damage::rule),
        DamageRuleSourceFilter.CODEC.optionalFieldOf("source",DamageRuleSourceFilter.ITEM).forGetter(Damage::source)).apply(i,Damage::new));
    MapCodec<Attribute> ATTRIBUTE = RecordCodecBuilder.mapCodec(i->i.group(
        NexusIdCodecs.CODEC.fieldOf("id").forGetter(Attribute::id),NexusIdCodecs.CODEC.fieldOf("attribute").forGetter(Attribute::attribute),
        Codec.DOUBLE.fieldOf("amount").forGetter(Attribute::amount),Codec.STRING.xmap(AttributeOperation::valueOf,Enum::name).fieldOf("operation").forGetter(Attribute::operation)).apply(i,Attribute::new));
    MapCodec<Behavior> BEHAVIOR = RecordCodecBuilder.mapCodec(i->i.group(NexusIdCodecs.CODEC.fieldOf("id").forGetter(Behavior::id),NexusIdCodecs.CODEC.fieldOf("definition").forGetter(Behavior::definition)).apply(i,Behavior::new));
    MapCodec<Effect> EFFECT = RecordCodecBuilder.mapCodec(i->i.group(NexusIdCodecs.CODEC.fieldOf("id").forGetter(Effect::id),NexusIdCodecs.CODEC.fieldOf("definition").forGetter(Effect::definition)).apply(i,Effect::new));
    MapCodec<Regeneration> REGENERATION = RecordCodecBuilder.mapCodec(i->i.group(NexusIdCodecs.CODEC.fieldOf("id").forGetter(Regeneration::id),NexusIdCodecs.CODEC.fieldOf("definition").forGetter(Regeneration::definition)).apply(i,Regeneration::new));
    MapCodec<Ability> ABILITY = RecordCodecBuilder.mapCodec(i->i.group(NexusIdCodecs.CODEC.fieldOf("id").forGetter(Ability::id),NexusIdCodecs.CODEC.fieldOf("definition").forGetter(Ability::definition)).apply(i,Ability::new));
    MapCodec<Shield> SHIELD = RecordCodecBuilder.mapCodec(i->i.group(NexusIdCodecs.CODEC.fieldOf("id").forGetter(Shield::id),NexusIdCodecs.CODEC.fieldOf("definition").forGetter(Shield::definition),Codec.DOUBLE.fieldOf("amount").forGetter(Shield::amount),Codec.INT.fieldOf("duration_ticks").forGetter(Shield::durationTicks)).apply(i,Shield::new));
    static MapCodec<? extends EntryContribution> codec(Kind kind) {
        return switch(kind) {case DAMAGE->DAMAGE;case ATTRIBUTE->ATTRIBUTE;case BEHAVIOR->BEHAVIOR;case EFFECT->EFFECT;case SHIELD->SHIELD;case REGENERATION->REGENERATION;case ABILITY->ABILITY;};
    }
    Codec<EntryContribution> CODEC = Codec.STRING.xmap(Kind::valueOf,Enum::name).dispatch("kind",EntryContribution::kind,EntryContribution::codec);
}
