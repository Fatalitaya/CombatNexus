package io.github.fatalitaya.combatnexus.combat.internal.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorDefinition;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.ability.api.AbilityExecutorSpec;
import io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityExecutorCatalog;
import io.github.fatalitaya.combatnexus.combat.config.CombatServerSettings;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.core.definition.DamageDefinitions;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.DatapackDamageRuleStore;
import io.github.fatalitaya.combatnexus.combat.damage.core.template.DamageTemplateSnapshot;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration;
import java.util.*;

/** Only detached values are saved. Compilation repeats executor, attribute and channel validation. */
public final class DefinitionBindingValues {
    private DefinitionBindingValues() {}
    private record Values(CombatServerSettings settings, DomainDefinitions domains,
            Map<NexusId, BehaviorDefinition> behaviors, Map<NexusId, CombatItemBindingSpec> items,
            Map<NexusId, AbilityExecutorSpec> executors, List<String> channels, List<String> rules,
            Map<NexusId, String> entries, Map<NexusId, String> affixes) {
        Values {
            Objects.requireNonNull(settings); Objects.requireNonNull(domains);
            behaviors = Map.copyOf(behaviors); items = Map.copyOf(items); executors = Map.copyOf(executors);
            channels = List.copyOf(channels); rules = List.copyOf(rules);
            entries = Map.copyOf(entries); affixes = Map.copyOf(affixes);
            if (behaviors.size() > 256 || items.size() > 4096 || executors.size() > 256
                    || channels.size() > 128 || rules.size() > 1024 || entries.size() > 1024 || affixes.size() > 1024)
                throw new IllegalArgumentException("Definition binding entry budget");
        }
    }
    private static <T> String encode(Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow().toString();
    }
    private static <T> T decode(Codec<T> codec, String value) {
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(value)).getOrThrow();
    }
    private static <T> Map<NexusId, String> encode(Codec<T> codec, Map<NexusId, T> values) {
        var result = new LinkedHashMap<NexusId, String>();
        values.forEach((id, value) -> result.put(id, encode(codec, value)));
        return result;
    }
    private static <T> Map<NexusId, T> decode(Codec<T> codec, Map<NexusId, String> values) {
        var result = new LinkedHashMap<NexusId, T>();
        values.forEach((id, value) -> result.put(id, decode(codec, value)));
        return result;
    }
    public static DefinitionBindingSnapshot capture(DefinitionGeneration generation) {
        var executors = new LinkedHashMap<NexusId, AbilityExecutorSpec>();
        generation.domains().abilities().values().forEach(a -> executors.put(a.executor(), AbilityExecutorCatalog.require(a.executor())));
        var values = new Values(generation.settings(), generation.domains(), generation.behaviors(), generation.itemBindings(), executors,
            generation.damage().channels().definitions().stream().map(c -> encode(DamageChannelRegistry.ChannelDefinition.CODEC, c)).toList(),
            generation.damage().rules().rules().stream().map(r -> encode(DamageRuleDefinition.CODEC, r)).toList(),
            encode(EntryDefinition.CODEC, generation.damage().templates().entries()),
            encode(AffixDefinition.CODEC, generation.damage().templates().affixes()));
        return new DefinitionBindingSnapshot(UUID.randomUUID(), CombatStateCodec.values().toJson(values));
    }
    public static DefinitionGeneration compile(DefinitionBindingSnapshot snapshot, long generation) {
        com.google.gson.JsonObject json;
        try { json = io.github.fatalitaya.combatnexus.config.SettingsJson.read(new java.io.StringReader(snapshot.content()), 1_048_576, 65_536); }
        catch (java.io.IOException failure) { throw new IllegalArgumentException("Invalid definition binding", failure); }
        io.github.fatalitaya.combatnexus.config.SettingsJson.fields(json, "settings", "domains", "behaviors", "items", "executors", "channels", "rules", "entries", "affixes");
        var values = Objects.requireNonNull(CombatStateCodec.values().fromJson(json, Values.class));
        values.executors().forEach((id, spec) -> {
            if (!spec.equals(AbilityExecutorCatalog.require(id))) throw new IllegalArgumentException("Bound executor declaration changed: " + id);
        });
        var channelValues = new LinkedHashMap<NexusId, DamageChannelRegistry.ChannelDefinition>();
        for (String encoded : values.channels()) {
            var channel = decode(DamageChannelRegistry.ChannelDefinition.CODEC, encoded);
            channel.resistanceAttribute().ifPresent(id -> {
                if (!io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().exists(id))
                    throw new IllegalArgumentException("Bound channel attribute unavailable: " + id);
            });
            if (channelValues.putIfAbsent(channel.channel(), channel) != null) throw new IllegalArgumentException("Duplicate bound channel");
        }
        var channels = DamageChannelRegistry.buildCandidate(channelValues, generation);
        return DamageChannelRegistry.withCandidate(channels, () -> {
            values.domains().shields().values().forEach(shield -> shield.channels().forEach(id -> {
                if (!DamageChannelRegistry.containsChannel(id)) throw new IllegalArgumentException("Unknown bound shield channel: " + id);
            }));
            var rules = values.rules().stream().map(r -> decode(DamageRuleDefinition.CODEC, r)).toList();
            var templates = new DamageTemplateSnapshot(generation, generation, true,
                decode(EntryDefinition.CODEC, values.entries()), decode(AffixDefinition.CODEC, values.affixes()));
            return new DefinitionGeneration(generation, values.settings(),
                new DamageDefinitions(channels, DatapackDamageRuleStore.Snapshot.create(generation, generation, true, rules), templates),
                values.behaviors(), values.domains(), values.items());
        });
    }
}
