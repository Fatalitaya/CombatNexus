package io.github.fatalitaya.combatnexus.combat.damage.core.template;

import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleLimits;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleReferenceValidator;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleValidator;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixValidator;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryValidator;

import java.util.List;

/** Strict validation shared by Java registration and datapack publication. */
public final class DamageTemplateDefinitionValidator {
    private DamageTemplateDefinitionValidator() {}

    public static void requireEntry(
            EntryDefinition definition,
            String source,
            boolean datapackReferences
    ) {
        if (definition == null) {
            throw new IllegalArgumentException("Entry template is null");
        }
        DamageRuleLimits.findItemProblem(List.of(definition), List.of())
                .ifPresent(reason -> {
                    throw new IllegalArgumentException(
                            "Invalid entry template: " + reason);
                });
        List<EntryDefinition> validated =
                EntryValidator.filterValid(List.of(definition), source);
        if (validated.size() != 1 || !validated.get(0).equals(definition)) {
            throw new IllegalArgumentException(
                    "Entry template did not pass strict validation: "
                            + definition.id());
        }
        for (var rule : definition.rules()) {
            boolean valid = datapackReferences
                    ? DamageRuleReferenceValidator.validateDatapackReferences(
                    rule, source, DamageRuleValidator.Policy.IGNORE)
                    : DamageRuleReferenceValidator
                    .validateJavaRegistrationReferences(
                            rule, source, DamageRuleValidator.Policy.IGNORE);
            if (!valid) {
                throw new IllegalArgumentException(
                        "Entry template has invalid rule references: "
                                + definition.id());
            }
        }
    }

    public static void requireAffix(
            AffixDefinition definition,
            String source,
            boolean datapackReferences
    ) {
        if (definition == null) {
            throw new IllegalArgumentException("Affix template is null");
        }
        DamageRuleLimits.findItemProblem(List.of(), List.of(definition))
                .ifPresent(reason -> {
                    throw new IllegalArgumentException(
                            "Invalid affix template: " + reason);
                });
        List<AffixDefinition> validated =
                AffixValidator.filterValid(List.of(definition), source);
        if (validated.size() != 1 || !validated.get(0).equals(definition)) {
            throw new IllegalArgumentException(
                    "Affix template did not pass strict validation: "
                            + definition.id());
        }
        for (var entry : definition.entries()) {
            for (var rule : entry.rules()) {
                boolean valid = datapackReferences
                        ? DamageRuleReferenceValidator
                        .validateDatapackReferences(
                                rule, source, DamageRuleValidator.Policy.IGNORE)
                        : DamageRuleReferenceValidator
                        .validateJavaRegistrationReferences(
                                rule, source, DamageRuleValidator.Policy.IGNORE);
                if (!valid) {
                    throw new IllegalArgumentException(
                            "Affix template has invalid rule references: "
                                    + definition.id());
                }
            }
        }
    }
}
