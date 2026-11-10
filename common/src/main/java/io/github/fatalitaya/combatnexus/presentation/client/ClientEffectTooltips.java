package io.github.fatalitaya.combatnexus.presentation.client;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import io.github.fatalitaya.combatnexus.presentation.api.DescriptionDocument;

public final class ClientEffectTooltips {
    private static final Map<NexusId, DescriptionDocument> ACTIVE = new HashMap<>();
    private ClientEffectTooltips() {}
    public static void clear() { ACTIVE.clear(); }
    public static Optional<DescriptionDocument> get(NexusId effect) { return Optional.ofNullable(ACTIVE.get(effect)); }
    public static void accept(NexusId effect, String json) {
        if (json.isEmpty()) { ACTIVE.remove(effect); return; }
        ACTIVE.remove(effect);
        try { ACTIVE.put(effect, DescriptionDocument.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow()); }
        catch (Exception failure) {}
    }
}
