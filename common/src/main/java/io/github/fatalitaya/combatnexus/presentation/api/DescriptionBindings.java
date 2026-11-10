package io.github.fatalitaya.combatnexus.presentation.api;

import java.util.*;

/** Explicit descriptor parameters only; no reflective field access or expression evaluation. */
public final class DescriptionBindings {
    private DescriptionBindings() {}

    public static DisplayText bind(DisplayText text, Map<String, DisplayArgument> parameters) {
        java.util.Objects.requireNonNull(text);
        if (text instanceof DisplayText.Literal literal) {
            return literal;
        } else if (text instanceof DisplayText.Translatable translated) {
            return new DisplayText.Translatable(
                    translated.key(),
                    translated.args().stream().map(a -> argument(a, parameters)).toList(),
                    translated.fallback());
        } else {
            throw new IllegalStateException("Unexpected sealed subtype");
        }
    }

    private static DisplayArgument argument(
            DisplayArgument argument, Map<String, DisplayArgument> parameters) {
        java.util.Objects.requireNonNull(argument);
        if (argument instanceof DisplayArgument.Binding binding) {
            var value =
                    Objects.requireNonNull(
                            parameters.get(binding.name()),
                            "Missing descriptor parameter: " + binding.name());
            if (value instanceof DisplayArgument.Binding)
                throw new IllegalArgumentException("Bindings cannot refer to bindings");
            return value;
        } else if (argument instanceof DisplayArgument.Text nested) {
            return DisplayArgument.text(bind(nested.value(), parameters));
        } else {
            return argument;
        }
    }
}
