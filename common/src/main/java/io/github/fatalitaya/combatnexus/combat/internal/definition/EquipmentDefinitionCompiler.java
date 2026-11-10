package io.github.fatalitaya.combatnexus.combat.internal.definition;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.combat.api.DomainDefinitions;
import io.github.fatalitaya.combatnexus.combat.api.affix.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.entry.*;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.api.source.SourcePlan;
import io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions;
import io.github.fatalitaya.combatnexus.effect.api.*;

import java.util.*;

/** Compiles both Java declarations and pack/stack values against one candidate generation. */
public final class EquipmentDefinitionCompiler {
    private EquipmentDefinitionCompiler() {}

    public record Contribution(NexusId id, SourcePlan plan) {}

    public record Compiled(
            CombatItemBindingSpec.Binding binding,
            EntryDefinition entry,
            AffixDefinition affix,
            List<Contribution> contributions) {
        public Compiled {
            contributions = List.copyOf(contributions);
            if ((entry == null) == (affix == null))
                throw new IllegalArgumentException("Expected entry or affix");
        }

        public List<EntryDefinition> entries() {
            return entry != null ? List.of(entry) : affix.entries();
        }
    }

    public static EntryDefinition entry(
            CombatItemBindingSpec.Binding binding, DamageItemDefinitions definitions) {
        return binding.entry()
                .orElseGet(
                        () ->
                                definitions
                                        .entry(binding.template().orElseThrow())
                                        .orElseThrow(
                                                () ->
                                                        new IllegalArgumentException(
                                                                "Unknown binding definition: "
                                                                        + binding.definition())));
    }

    public static AffixDefinition affix(
            CombatItemBindingSpec.Binding binding, DamageItemDefinitions definitions) {
        return binding.affix()
                .orElseGet(
                        () ->
                                definitions
                                        .affix(binding.template().orElseThrow())
                                        .orElseThrow(
                                                () ->
                                                        new IllegalArgumentException(
                                                                "Unknown binding definition: "
                                                                        + binding.definition())))
                .resolve(definitions::entry);
    }

    public static Compiled compile(
            CombatItemBindingSpec.Binding binding,
            DamageItemDefinitions definitions,
            Map<NexusId, BehaviorDefinition> behaviors,
            DomainDefinitions domains) {
        if (definitions instanceof DefinitionGeneration generation
                && io.github.fatalitaya.combatnexus.combat.damage.core.registry
                                .DamageChannelRegistry.contentRevision()
                        != generation.damage().channels().contentRevision())
            return io.github.fatalitaya.combatnexus.combat.damage.core.registry
                    .DamageChannelRegistry.withCandidate(
                    generation.damage().channels(),
                    () -> compileValues(binding, definitions, behaviors, domains));
        return compileValues(binding, definitions, behaviors, domains);
    }

    private static Compiled compileValues(
            CombatItemBindingSpec.Binding binding,
            DamageItemDefinitions definitions,
            Map<NexusId, BehaviorDefinition> behaviors,
            DomainDefinitions domains) {
        var out = new ArrayList<Contribution>();
        EntryDefinition entry = null;
        AffixDefinition affix = null;
        if (binding.kind() == CombatItemBindingSpec.Kind.ENTRY) {
            entry = entry(binding, definitions);
            append(entry, "entry", behaviors, domains, out);
        } else {
            affix = affix(binding, definitions);
            validateAffixStructure(affix);
            for (var member : affix.members())
                append(
                        member.entry().orElseThrow(),
                        member.id().namespace() + "/" + member.id().path(),
                        behaviors,
                        domains,
                        out);
        }
        if (out.size() > 64)
            throw new IllegalArgumentException("Equipment contribution budget exceeded");
        return new Compiled(binding, entry, affix, out);
    }

    private static void validateAffixStructure(AffixDefinition affix) {
        io.github.fatalitaya.combatnexus.combat.damage.core.template
                .DamageTemplateDefinitionValidator.requireAffix(
                affix, "equipment/" + affix.id(), true);
        if (affix.entries().stream().mapToInt(e -> e.contributions().size()).sum() > 64)
            throw new IllegalArgumentException("Affix contribution budget exceeded");
    }

    public static void validate(
            AffixDefinition affix,
            Map<NexusId, BehaviorDefinition> behaviors,
            DomainDefinitions domains) {
        validateAffixStructure(affix);
        for (var entry : affix.entries()) validate(entry, behaviors, domains);
    }

    public static void validate(
            EntryDefinition entry,
            Map<NexusId, BehaviorDefinition> behaviors,
            DomainDefinitions domains) {
        append(entry, "entry", behaviors, domains, new ArrayList<>());
    }

    private static <T> T required(Map<NexusId, T> definitions, NexusId id) {
        T value = definitions.get(id);
        if (value == null)
            throw new IllegalArgumentException("Unknown contribution definition: " + id);
        return value;
    }

    private static void append(
            EntryDefinition entry,
            String member,
            Map<NexusId, BehaviorDefinition> behaviors,
            DomainDefinitions domains,
            List<Contribution> out) {
        io.github.fatalitaya.combatnexus.combat.damage.core.template
                .DamageTemplateDefinitionValidator.requireEntry(
                entry, "equipment/" + entry.id(), true);
        for (var c : entry.contributions()) {
            var key =
                    new NexusId(
                            "combatnexus", member + "/" + c.id().namespace() + "/" + c.id().path());
            SourcePlan plan = sourcePlan(entry, key, c, behaviors, domains);
            if (plan != null) out.add(new Contribution(key, plan));
        }
    }

    private static SourcePlan sourcePlan(
            EntryDefinition entry,
            NexusId key,
            EntryContribution c,
            Map<NexusId, BehaviorDefinition> behaviors,
            DomainDefinitions domains) {
        java.util.Objects.requireNonNull(c);
        if (c instanceof EntryContribution.Damage ignored) {
            return null;
        } else if (c instanceof EntryContribution.Attribute a) {
            var effect =
                    new EffectDefinition(
                            0,
                            EffectLifetime.permanent(),
                            EffectService.Stacking.REPLACE,
                            EffectPolicy.DEFAULT,
                            List.of(
                                    new EffectInstance.AttributeContribution(
                                            a.attribute(),
                                            new AttributeSnapshot.Contribution(
                                                    a.id(), a.amount(), a.operation()))),
                            BehaviorPlan.NONE,
                            Set.of());
            return new SourcePlan(
                    entry.id(), Map.of(key, effect), Map.of(), Map.of(), Map.of(), true);
        } else if (c instanceof EntryContribution.Behavior a) {
            var d = required(behaviors, a.definition());
            var effect =
                    new EffectDefinition(
                            0,
                            d.lifetime(),
                            EffectService.Stacking.REPLACE,
                            EffectPolicy.DEFAULT,
                            List.of(),
                            d.plan(),
                            Set.of());
            return new SourcePlan(
                    a.definition(),
                    Map.of(a.definition(), effect),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    true);
        } else if (c instanceof EntryContribution.Effect a) {
            return new SourcePlan(
                    a.definition(),
                    Map.of(a.definition(), required(domains.effects(), a.definition())),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    true);
        } else if (c instanceof EntryContribution.Shield a) {
            return new SourcePlan(
                    a.definition(),
                    Map.of(),
                    Map.of(),
                    Map.of(
                            a.id(),
                            new SourcePlan.Shield(
                                    required(domains.shields(), a.definition()),
                                    Math.min(
                                            a.amount(),
                                            required(domains.shields(), a.definition()).capacity()),
                                    a.durationTicks(),
                                    SourcePlan.ShieldRemoval.REVOKE)),
                    Map.of(),
                    true);
        } else if (c instanceof EntryContribution.Regeneration a) {
            return new SourcePlan(
                    a.definition(),
                    Map.of(),
                    Map.of(a.id(), required(domains.regeneration(), a.definition())),
                    Map.of(),
                    Map.of(),
                    true);
        } else if (c instanceof EntryContribution.Ability a) {
            required(domains.abilities(), a.definition());
            return new SourcePlan(
                    a.definition(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(a.id(), a.definition()),
                    true);
        } else {
            throw new IllegalStateException("Unexpected sealed subtype");
        }
    }
}
