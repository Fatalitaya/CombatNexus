package io.github.fatalitaya.combatnexus.effect.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.HashSet;
import java.util.Set;

public final class VanillaEffectContributions {
    private static final float EPSILON = 0.0001f;

    private VanillaEffectContributions() {
    }

    public record OffensiveObservation(double currentValue, double withoutBoth, double withoutWeakness,
            boolean strengthPresent, boolean weaknessPresent) {}

    public static OffensiveMobEffectBreakdown computeBreakdown(OffensiveObservation observation) {
        if (!observation.strengthPresent() && !observation.weaknessPresent()) return OffensiveMobEffectBreakdown.NONE;
        double currentValue = observation.currentValue();
        double valueWithoutBoth = observation.withoutBoth();

        float observedDelta =
                finiteDelta(currentValue, valueWithoutBoth);

        /*
         * observedDelta is the authoritative value used for vanilla base
         * reconstruction.
         *
         * strengthDelta / weaknessDelta are debug-facing approximations. If a
         * mod changes Strength or Weakness to use multiplicative operations, the
         * exact split can be order-dependent, but observedDelta still remains
         * the value DN needs.
         */
        double valueWithoutWeakness = observation.withoutWeakness();

        float strengthDelta = !observation.strengthPresent()
                ? 0.0f
                : finiteDelta(valueWithoutWeakness, valueWithoutBoth);

        float weaknessDelta = !observation.weaknessPresent()
                ? 0.0f
                : observedDelta - strengthDelta;

        float enabledDelta = observedDelta;

        return new OffensiveMobEffectBreakdown(
                strengthDelta,
                weaknessDelta,
                observedDelta,
                enabledDelta
        );
    }

    private static float finiteDelta(
            double valueWithModifier,
            double valueWithoutModifier
    ) {
        double delta = valueWithModifier - valueWithoutModifier;

        if (!Double.isFinite(delta)) {
            return 0.0f;
        }

        if (Math.abs(delta) <= EPSILON) {
            return 0.0f;
        }

        return (float) delta;
    }

    public record OffensiveMobEffectBreakdown(
            float strengthDelta,
            float weaknessDelta,
            float observedDelta,
            float enabledDelta
    ) {
        public static final OffensiveMobEffectBreakdown NONE =
                new OffensiveMobEffectBreakdown(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                );

        public boolean hasEnabledDelta() {
            return Math.abs(enabledDelta) > EPSILON;
        }

        public boolean hasObservedDelta() {
            return Math.abs(observedDelta) > EPSILON;
        }

        public boolean hasObservedWeaknessDelta() {
            return Math.abs(weaknessDelta) > EPSILON;
        }
    }
}
