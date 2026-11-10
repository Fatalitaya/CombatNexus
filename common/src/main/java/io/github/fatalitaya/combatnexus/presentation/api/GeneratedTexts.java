package io.github.fatalitaya.combatnexus.presentation.api;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Generated descriptions accept only scalar text, numbers and the detached display model. */
public final class GeneratedTexts {
    private GeneratedTexts() {}
    public static DisplayText literal(String value) {
        return value.isBlank() ? new DisplayText.Translatable("tooltip.combatnexus.text.literal", List.of(new DisplayArgument.Literal(value)), Optional.of("%s")) : DisplayText.literal(value);
    }
    public static DisplayText translatable(String key, Object... arguments) {
        return new DisplayText.Translatable(key, Arrays.stream(arguments).map(GeneratedTexts::argument).toList(), Optional.empty());
    }
    public static DisplayText translatableWithFallback(String key, String fallback, Object... arguments) {
        return new DisplayText.Translatable(key, Arrays.stream(arguments).map(GeneratedTexts::argument).toList(), Optional.ofNullable(fallback));
    }
    private static DisplayArgument argument(Object value) {
        if (value instanceof DisplayText text) return DisplayArgument.text(text);
        if (value instanceof Number number) return DisplayArgument.number(number.doubleValue());
        if (value instanceof String text) return new DisplayArgument.Literal(text);
        throw new IllegalArgumentException("Unsupported generated description argument");
    }
    public static DisplayText join(List<DisplayText> values, String separator) {
        if (values.isEmpty()) return literal("");
        DisplayText result = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (!separator.isEmpty()) result = concat(result, literal(separator));
            result = concat(result, values.get(i));
        }
        return result;
    }
    private static DisplayText concat(DisplayText first, DisplayText second) {
        return DisplayText.translated("tooltip.combatnexus.text.concat", DisplayArgument.text(first), DisplayArgument.text(second));
    }
}
