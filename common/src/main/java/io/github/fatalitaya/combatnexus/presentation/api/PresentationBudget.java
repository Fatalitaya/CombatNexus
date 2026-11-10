package io.github.fatalitaya.combatnexus.presentation.api;

/** Presentation work may be refused; authoritative battle capture never uses this budget. */
public final class PresentationBudget {
    private int entries, bytes;
    private final int maximumEntries, maximumBytes;
    public PresentationBudget(int maximumEntries, int maximumBytes) {
        this.maximumEntries = maximumEntries; this.maximumBytes = maximumBytes;
    }
    public void entry(int count) {
        if (count < 0 || count > maximumEntries - entries) throw new Exceeded();
        entries += count;
    }
    public void encoded(String text) {
        if (text.length() > 4096) throw new Exceeded();
        int size = text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (size > maximumBytes - bytes) throw new Exceeded();
        bytes += size;
    }
    /** Counts JSON bytes without serializing unchanged presentation content. */
    public void encoded(com.google.gson.JsonElement value) {
        int size = jsonBytes(value);
        if (size > maximumBytes - bytes) throw new Exceeded();
        bytes += size;
    }
    private int jsonBytes(com.google.gson.JsonElement value) {
        long size = 0;
        if (value.isJsonNull()) return 4;
        if (value.isJsonPrimitive()) {
            var primitive = value.getAsJsonPrimitive();
            return primitive.isString() ? stringBytes(primitive.getAsString()) : primitive.toString().length();
        }
        if (value.isJsonArray()) {
            size = 2;
            for (var child : value.getAsJsonArray()) size += jsonBytes(child) + 1L;
        } else {
            size = 2;
            for (var entry : value.getAsJsonObject().entrySet()) size += stringBytes(entry.getKey()) + jsonBytes(entry.getValue()) + 2L;
        }
        if (size > maximumBytes) throw new Exceeded();
        return (int)size;
    }
    private int stringBytes(String value) {
        if (value.length() > 4096) throw new Exceeded();
        int size = 2;
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (c == '"' || c == '\\') size += 2;
            else if (c < 32 || c == '\u2028' || c == '\u2029') size += 6;
            else if (c < 128) size++;
            else if (c < 2048) size += 2;
            else if (Character.isHighSurrogate(c) && index + 1 < value.length() && Character.isLowSurrogate(value.charAt(index + 1))) { size += 4; index++; }
            else size += 3;
        }
        return size;
    }
    public static final class Exceeded extends RuntimeException {
        public Exceeded() { super("Presentation budget exceeded"); }
    }
}
