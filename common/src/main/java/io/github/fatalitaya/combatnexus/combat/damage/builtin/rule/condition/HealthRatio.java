package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

final class HealthRatio {
    private HealthRatio() {}
    static final Codec<Float> CODEC = Codec.FLOAT.validate(value -> valid(value)
            ? DataResult.success(value) : DataResult.error(() -> "Health ratio must be finite and in [0,1]"));
    private static boolean valid(float value) { return Float.isFinite(value) && value >= 0 && value <= 1; }
    static void require(float value) {
        if (!valid(value)) throw new IllegalArgumentException("Health ratio must be finite and in [0,1]");
    }
}
