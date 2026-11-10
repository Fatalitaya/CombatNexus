package io.github.fatalitaya.combatnexus.combat.damage.api.display;

import java.util.*;

/** Cold formatting only; never samples an entity or a newer definition generation. */
public final class ContributionProjector {
    private ContributionProjector() {}
    public static DamageContributionDescriptor project(ContributionRecord record) {
        if (record instanceof ContributionFact fact) return rule(fact);
        if (record instanceof DefensePenetrationFact fact) {
            return new DamageContributionDescriptor(fact.source(), DamageContributionSourceKind.JAVA_API,
                    DamageContributionOperationKind.PENETRATE_DEFENSE_INPUT,
                    io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase.MITIGATION_SETUP,
                    Optional.empty(), Optional.empty(), Optional.empty(), fact.before() - fact.after(), fact.status(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    List.of("input=" + fact.input(), "ratio=" + fact.ratio(), "flat=" + fact.flat(),
                            "before=" + fact.before(), "after=" + fact.after()), true);
        }
        var fact = (VanillaContributionFact)record;
        var projected = switch(fact.kind()) {
            case BASE -> VanillaContributionProjector.vanillaBase(fact.id(), fact.sourceKind(), fact.phase(), fact.channel(), fact.bucket(), fact.value(), fact.sourceLabel());
            case MULTIPLIER -> VanillaContributionProjector.vanillaMultiplier(fact.id(), fact.phase(), fact.channel(), fact.bucket(), fact.preMultiplierBucket(), fact.value(), fact.sourceLabel());
            case RESISTANCE -> VanillaContributionProjector.vanillaTemporaryResistance(fact.id(), fact.phase(), fact.channel(), fact.value(), fact.sourceLabel());
            case MITIGATION -> VanillaContributionProjector.vanillaMitigation(fact.id(), fact.phase(), fact.channel(), fact.value(), fact.sourceLabel());
            case ARMOR -> VanillaContributionProjector.vanillaArmorEffectiveness(fact.id(), fact.phase(), fact.value(), fact.sourceLabel());
        };
        return projected.withStatus(fact.status());
    }
    private static DamageContributionDescriptor rule(ContributionFact fact) {
        var details = fact.details();
        List<String> lines = new ArrayList<>();
        lines.add("rule=" + details.rule()); lines.add("operation=" + details.operation());
        fact.channel().ifPresent(value -> lines.add("channel=" + value));
        fact.applicationBucket().ifPresent(value -> lines.add("application_bucket=" + value));
        fact.preMultiplierBucket().ifPresent(value -> lines.add("pre_multiplier_bucket=" + value));
        lines.add("owner_kind=" + details.ownerKind());
        details.affix().ifPresent(value -> lines.add("affix=" + value));
        details.installationSource().ifPresent(value -> lines.add("installation_source="+value));
        details.entry().ifPresent(value -> lines.add("entry=" + value));
        if (details.provider() != null) {
            lines.add("provider=" + details.provider()); lines.add("source_location=" + details.location());
            lines.add("role=" + details.role());
        }
        lines.add("result=" + details.result());
        return new DamageContributionDescriptor(fact.id(), fact.sourceKind(), fact.operationKind(), fact.phase(),
                fact.channel(), fact.applicationBucket(), fact.preMultiplierBucket(), fact.value(), fact.status(),
                fact.displayGroup(), fact.displaySubgroup(), Optional.empty(), lines, true);
    }
}
