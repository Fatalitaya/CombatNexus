package io.github.fatalitaya.combatnexus.combat.internal.definition;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.config.CombatServerSettings;
import io.github.fatalitaya.combatnexus.combat.damage.core.definition.DamageDefinitions;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorDefinition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
/** Complete immutable resource generation. Requests retain this exact handle. */
public record DefinitionGeneration(long generation, CombatServerSettings settings,
        DamageDefinitions damage, Map<NexusId, BehaviorDefinition> behaviors, DomainDefinitions domains, Map<NexusId,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec> itemBindings) implements io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions {
    public DefinitionGeneration(long generation,CombatServerSettings settings,DamageDefinitions damage,Map<NexusId,BehaviorDefinition> behaviors,DomainDefinitions domains,Map<NexusId,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec> itemBindings){
        if(generation<1)throw new IllegalArgumentException("Invalid generation");
        this.generation=generation;this.settings=Objects.requireNonNull(settings);this.damage=Objects.requireNonNull(damage);this.behaviors=Map.copyOf(behaviors);this.domains=Objects.requireNonNull(domains);this.itemBindings=Map.copyOf(itemBindings);
        java.util.function.Supplier<Boolean> validate=()->{
            domains.abilities().values().forEach(io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityExecutorCatalog::validate);
            for(var entry:damage.templates().entries().values())EquipmentDefinitionCompiler.validate(entry,behaviors,domains);
            for(var affix:damage.templates().affixes().values())EquipmentDefinitionCompiler.validate(affix,behaviors,domains);
            this.itemBindings.values().forEach(this::validateBindings);return true;
        };
        if(io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry.contentRevision()==damage.channels().contentRevision())validate.get();
        else io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry.withCandidate(damage.channels(),validate);
    }
    public void validateBindings(io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec spec){
        for(var binding:spec.bindings())EquipmentDefinitionCompiler.compile(binding,this,behaviors,domains);
    }
    public io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec bindings(io.github.fatalitaya.combatnexus.combat.damage.api.item.RuleItemView item){
        var spec=item.bindingPatch().apply(itemBindings.getOrDefault(item.itemId(),item.defaultBindings()));validateBindings(spec);return spec;
    }
    public Optional<io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition> entry(NexusId id) { return damage.templates().entry(Objects.requireNonNull(id)); }
    public Optional<io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition> affix(NexusId id) { return damage.templates().affix(Objects.requireNonNull(id)); }
    public boolean templatesReady() { return damage.templates().isCompatibleWith(damage.channels().contentRevision()); }
    public long templateChannelRevision() { return damage.templates().validatedChannelRevision(); }
    public io.github.fatalitaya.combatnexus.combat.damage.api.item.ItemCombatPresentationSnapshot itemPresentation(io.github.fatalitaya.combatnexus.combat.damage.api.item.RuleItemView item) {
        var rules = io.github.fatalitaya.combatnexus.combat.damage.core.security.DamageModuleItemSecurity.validateForExecution(item, "presentation", this);
        return new io.github.fatalitaya.combatnexus.combat.damage.api.item.ItemCombatPresentationSnapshot(generation, rules.entries(), rules.affixes());
    }
}
