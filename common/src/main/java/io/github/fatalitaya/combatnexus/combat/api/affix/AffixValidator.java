package io.github.fatalitaya.combatnexus.combat.api.affix;

import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryValidator;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleLimits;

import java.util.ArrayList;
import java.util.List;

public final class AffixValidator {
    private AffixValidator() {
    }

    public static List<AffixDefinition> filterValid(
            List<AffixDefinition> affixes,
            String source
    ) {
        if (affixes == null || affixes.isEmpty()) {
            return List.of();
        }

        List<AffixDefinition> result = new ArrayList<>();

        for (AffixDefinition affix : affixes) {
            if (affix == null) {
                continue;
            }

            java.util.Optional<String> structuralProblem =
                    DamageRuleLimits.findItemProblem(
                            List.of(),
                            List.of(affix)
                    );

            if (structuralProblem.isPresent()) {
                continue;
            }

            if (affix.entries().isEmpty()) {
                continue;
            }

            List<EntryDefinition> validEntries =
                    EntryValidator.filterValid(
                            affix.entries(),
                            source + "/affix/" + affix.id()
                    );

            if (validEntries.isEmpty()) {
                continue;
            }

            result.add(new AffixDefinition(
                    affix.id(),
                    affix.display(),
                    affix.rarity(),
                    affix.members(),
                    affix.stacking(),
                    affix.stackingGroup()
            ));
        }

        return List.copyOf(result);
    }

}
