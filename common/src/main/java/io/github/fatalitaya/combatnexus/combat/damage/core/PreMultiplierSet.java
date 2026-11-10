package io.github.fatalitaya.combatnexus.combat.damage.core;

import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

/**
 * Compact additive pre-multiplier accumulator.
 * <p>
 * DamageModule pre multipliers use bucketed additive stacking:
 * all values written to the same pre-multiplier bucket are added first,
 * then each bucket is applied multiplicatively.
 */
public final class PreMultiplierSet {

    private final PreMultiplierLayout layout;
    private float[] values;

    public PreMultiplierSet(PreMultiplierLayout layout) {
        this.layout = java.util.Objects.requireNonNull(layout);
    }

    @ApiStatus.Internal
    public PreMultiplierSet copy() {
        PreMultiplierSet copy = new PreMultiplierSet(layout);
        copy.values = values == null
                ? null
                : Arrays.copyOf(values, values.length);
        return copy;
    }

    public static float applyCombined(
            float amount,
            PreMultiplierSet first,
            PreMultiplierSet second
    ) {
        if ((first == null || first.isEmpty())
                && (second == null || second.isEmpty())) {
            return amount;
        }


        var layout = first != null ? first.layout : second.layout;
        return io.github.fatalitaya.combatnexus.formula.MultiplierFormula.applyBuckets(
                amount, layout.size(),
                i -> first == null ? 0.0f : first.getOrZero(i),
                i -> second == null ? 0.0f : second.getOrZero(i));
    }

    private void validateId(int preMultiplierBucketId) {
        int count = layout.size();

        if (preMultiplierBucketId < 0 || preMultiplierBucketId >= count) {
            throw new IndexOutOfBoundsException(
                    "Invalid pre-multiplier bucket id: " + preMultiplierBucketId
            );
        }
    }

    public void add(int preMultiplierBucketId, float value) {
        validateId(preMultiplierBucketId);

        if (value == 0.0f) {
            return;
        }

        ensureCapacity();
        values[preMultiplierBucketId] += value;
    }

    public float getOrZero(int preMultiplierBucketId) {
        if (values == null
                || preMultiplierBucketId < 0
                || preMultiplierBucketId >= values.length) {
            return 0.0f;
        }

        return values[preMultiplierBucketId];
    }

    public boolean isEmpty() {
        if (values == null) {
            return true;
        }

        int count = Math.min(values.length, layout.size());
        for (int i = 0; i < count; i++) {
            if (values[i] != 0.0f) {
                return false;
            }
        }

        return true;
    }

    public float apply(float amount) {
        if (values == null) {
            return amount;
        }

        return io.github.fatalitaya.combatnexus.formula.MultiplierFormula.applyBuckets(
                amount, Math.min(values.length, layout.size()),
                i -> values[i], i -> 0.0f);
    }

    private void ensureCapacity() {
        int count = layout.size();

        if (values != null && values.length == count) {
            return;
        }

        float[] next = new float[count];

        if (values != null) {
            System.arraycopy(values, 0, next, 0, Math.min(values.length, next.length));
        }

        values = next;
    }
}

