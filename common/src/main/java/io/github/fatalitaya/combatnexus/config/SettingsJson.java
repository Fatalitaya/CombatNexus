package io.github.fatalitaya.combatnexus.config;

import com.google.gson.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.util.Set;

/** Strict, bounded JSON input shared by local files and datapack settings. */
public final class SettingsJson {
    public static final int MAX_CHARACTERS = 16_384;
    private SettingsJson() {}
    public static JsonObject read(Reader reader) throws IOException {
        return read(reader, MAX_CHARACTERS, 4096);
    }
    public static JsonObject read(Reader reader, int maximumCharacters, int maximumNodes) throws IOException {
        if (maximumCharacters < 1 || maximumNodes < 1) throw new IllegalArgumentException("Invalid JSON budget");
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[1024];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            if (text.length() + count > maximumCharacters) throw new IllegalArgumentException("JSON exceeds " + maximumCharacters + " characters");
            text.append(buffer, 0, count);
        }
        try (JsonReader input = new JsonReader(new StringReader(text.toString()))) {
            input.setStrictness(Strictness.STRICT);
            JsonObject value = readValue(input, 0, new int[]{0}, maximumNodes).getAsJsonObject();
            if (input.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing JSON content");
            return value;
        }
    }
    private static JsonElement readValue(JsonReader input, int depth, int[] nodes, int maximumNodes) throws IOException {
        if (depth > 32 || ++nodes[0] > maximumNodes) throw new IllegalArgumentException("JSON structure budget exceeded");
        return switch (input.peek()) {
            case BEGIN_OBJECT -> {
                input.beginObject(); var object = new JsonObject();
                while (input.hasNext()) {
                    String name = input.nextName();
                    if (object.has(name)) throw new IllegalArgumentException("Duplicate JSON key: " + name);
                    object.add(name, readValue(input, depth + 1, nodes, maximumNodes));
                }
                input.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                input.beginArray(); var array = new JsonArray();
                while (input.hasNext()) array.add(readValue(input, depth + 1, nodes, maximumNodes));
                input.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(input.nextString());
            case NUMBER -> new JsonPrimitive(new java.math.BigDecimal(input.nextString()));
            case BOOLEAN -> new JsonPrimitive(input.nextBoolean());
            case NULL -> { input.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IllegalArgumentException("Expected JSON value");
        };
    }
    public static JsonObject fields(JsonObject value, String... names) {
        if (!value.keySet().equals(Set.of(names))) {
            throw new IllegalArgumentException("Expected exactly these settings keys: " + String.join(", ", names));
        }
        return value;
    }
    public static float number(JsonObject value, String name) {
        JsonPrimitive primitive = value.getAsJsonPrimitive(name);
        if (!primitive.isNumber()) throw new IllegalArgumentException(name + " must be a number");
        float result = primitive.getAsFloat();
        if (!Float.isFinite(result)) throw new IllegalArgumentException(name + " must be finite");
        return result;
    }
    public static int integer(JsonObject value, String name) {
        JsonPrimitive primitive = value.getAsJsonPrimitive(name);
        if (!primitive.isNumber()) throw new IllegalArgumentException(name + " must be an integer");
        return primitive.getAsBigDecimal().intValueExact();
    }
    public static boolean bool(JsonObject value, String name) {
        JsonPrimitive primitive = value.getAsJsonPrimitive(name);
        if (!primitive.isBoolean()) throw new IllegalArgumentException(name + " must be a boolean");
        return primitive.getAsBoolean();
    }
    public static <E extends Enum<E>> E enumeration(JsonObject value, String name, Class<E> type) {
        JsonPrimitive primitive = value.getAsJsonPrimitive(name);
        if (!primitive.isString()) throw new IllegalArgumentException(name + " must be a string");
        return Enum.valueOf(type, primitive.getAsString());
    }
}
