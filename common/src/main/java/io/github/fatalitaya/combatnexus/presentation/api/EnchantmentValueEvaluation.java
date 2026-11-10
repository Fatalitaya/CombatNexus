package io.github.fatalitaya.combatnexus.presentation.api;

import com.google.gson.JsonElement;

/** Evaluates the current target's native level-based value grammar. */
@FunctionalInterface
public interface EnchantmentValueEvaluation {
    float evaluate(JsonElement encoded, int level);
}
