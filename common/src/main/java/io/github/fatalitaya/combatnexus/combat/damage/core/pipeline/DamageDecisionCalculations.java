package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecision;
import io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecisionOutcome;

import java.util.Optional;

/** Pure decisions; the platform owns sampling and records every consumed sample. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class DamageDecisionCalculations {
    private DamageDecisionCalculations() {}

    public record CriticalResolution(Optional<CriticalDecisionOutcome> outcome) {
        public CriticalResolution {
            java.util.Objects.requireNonNull(outcome);
        }

        public boolean requiresSampling() {
            return outcome.isEmpty();
        }

        private static CriticalResolution decided(CriticalDecisionOutcome outcome) {
            return new CriticalResolution(Optional.of(outcome));
        }
    }

    public static CriticalResolution resolveCritical(
            CriticalDecision decision,
            boolean melee,
            boolean projectile,
            boolean suppressed,
            boolean player) {
        if (decision == CriticalDecision.SUPPRESS_CRITICAL)
            return CriticalResolution.decided(CriticalDecisionOutcome.SUPPRESSED);
        if (decision == CriticalDecision.FORCE_CRITICAL)
            return CriticalResolution.decided(
                    melee
                            ? CriticalDecisionOutcome.VANILLA_MELEE
                            : projectile
                                    ? CriticalDecisionOutcome.VANILLA_PROJECTILE
                                    : CriticalDecisionOutcome.FORCED);
        if (projectile)
            return CriticalResolution.decided(CriticalDecisionOutcome.VANILLA_PROJECTILE);
        if (melee && !suppressed)
            return CriticalResolution.decided(CriticalDecisionOutcome.VANILLA_MELEE);
        if (!player || suppressed)
            return CriticalResolution.decided(CriticalDecisionOutcome.INELIGIBLE);
        return new CriticalResolution(Optional.empty());
    }

    public static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    public static float criticalChance(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, Math.min(1.0f, value)) : 0.0f;
    }

    public static boolean critical(float chance, float sample) {
        return Float.isFinite(sample) && sample < criticalChance(chance);
    }

    public static boolean dodgeRequiresSample(float chance) {
        return Float.isFinite(chance) && chance > 0 && chance < 1;
    }

    public static boolean dodge(float chance, float sample) {
        return Float.isFinite(chance) && chance > 0 && (!(chance < 1) || !(sample >= chance));
    }
}
