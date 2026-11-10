package io.github.fatalitaya.combatnexus.presentation.client;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.presentation.api.DescriptionDocument;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Connection-wide definitions. ItemStacks retain only their vanilla-synchronized components. */
public final class ClientTooltipDefinitions {
    private static long generation;
    private static final Map<NexusId,CombatItemBindingSpec> BINDINGS=new HashMap<>();
    private static final Set<NexusId> INVALID_BINDINGS=new HashSet<>();
    public static CombatItemBindingSpec bindings(NexusId item,CombatItemBindingSpec defaults,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingPatch patch){if(INVALID_BINDINGS.contains(item))throw new IllegalArgumentException("Unavailable item bindings");return patch.apply(BINDINGS.getOrDefault(item,defaults));}
    private static final Map<NexusId, DescriptionDocument> ENTRIES = new HashMap<>();
    private static final Map<NexusId, DescriptionDocument> AFFIXES = new HashMap<>();
    private static final Map<NexusId, DescriptionDocument> EFFECTS = new HashMap<>(), BEHAVIORS = new HashMap<>();
    private static final Map<NexusId, DescriptionDocument> SHIELDS=new HashMap<>(), REGENERATION=new HashMap<>(), ABILITIES=new HashMap<>();
    private static final Map<NexusId, DescriptionDocument> ITEM_EFFECTS = new HashMap<>();
    private ClientTooltipDefinitions() {}
    public static void begin(long next) {
        if (next <= generation) return;
        clear(); generation = next;
    }
    public enum Kind { ENTRY, AFFIX, EFFECT, BEHAVIOR, ITEM_EFFECT, BINDINGS, SHIELD, REGENERATION, ABILITY }
    public static void accept(long incomingGeneration, Kind kind, NexusId id, String json) {
        if (incomingGeneration != generation) return;
        if(kind==Kind.BINDINGS){
            BINDINGS.remove(id);INVALID_BINDINGS.add(id);
            try{BINDINGS.put(id,CombatItemBindingSpec.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(json)).getOrThrow());INVALID_BINDINGS.remove(id);}catch(RuntimeException failure){}
            return;
        }
        var destination = switch (kind) { case ENTRY -> ENTRIES; case AFFIX -> AFFIXES; case EFFECT -> EFFECTS; case BEHAVIOR -> BEHAVIORS; case ITEM_EFFECT -> ITEM_EFFECTS; case SHIELD -> SHIELDS;case REGENERATION -> REGENERATION; case ABILITY -> ABILITIES; case BINDINGS -> throw new IllegalStateException(); };
        destination.remove(id);
        if (json.isEmpty()) return;
        try {
            destination.put(id, DescriptionDocument.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow());
        } catch (Exception failure) {
        }
    }

    public static Optional<DescriptionDocument> entry(NexusId id) { return Optional.ofNullable(ENTRIES.get(id)); }
    public static Optional<DescriptionDocument> affix(NexusId id) { return Optional.ofNullable(AFFIXES.get(id)); }
    public static Optional<DescriptionDocument> effect(NexusId id, Kind kind) {
        return Optional.ofNullable((switch(kind){case EFFECT->EFFECTS;case BEHAVIOR->BEHAVIORS;case SHIELD->SHIELDS;case REGENERATION->REGENERATION;case ABILITY->ABILITIES;default->throw new IllegalArgumentException("Not a contribution kind");}).get(id));
    }
    public static long generation() { return generation; }
    public static Optional<DescriptionDocument> itemEffect(NexusId id) { return Optional.ofNullable(ITEM_EFFECTS.get(id)); }
    public static void clear() { generation = 0; BINDINGS.clear(); INVALID_BINDINGS.clear(); ENTRIES.clear(); AFFIXES.clear(); EFFECTS.clear(); BEHAVIORS.clear(); SHIELDS.clear();REGENERATION.clear();ABILITIES.clear(); ITEM_EFFECTS.clear(); }
}
