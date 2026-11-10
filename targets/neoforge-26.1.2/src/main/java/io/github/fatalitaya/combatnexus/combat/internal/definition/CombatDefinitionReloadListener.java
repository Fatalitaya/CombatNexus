package io.github.fatalitaya.combatnexus.combat.internal.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.config.CombatServerSettings;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.DatapackDamageRuleReloadListener;
import io.github.fatalitaya.combatnexus.combat.damage.core.template.DatapackDamageTemplateReloadListener;
import io.github.fatalitaya.combatnexus.config.SettingsJson;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.config.DefinitionResources;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Strict decode and candidate compilation; apply does not publish executable state. */
public final class CombatDefinitionReloadListener {
    private final Map<NexusId,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec> defaults;
    public CombatDefinitionReloadListener(){this(Map.of());}
    public CombatDefinitionReloadListener(Map<NexusId,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec> defaults){this.defaults=Map.copyOf(defaults);}
    public record Prepared(CombatServerSettings settings,
            Map<NexusId, DamageChannelRegistry.ChannelDefinition> channels,
            Map<NexusId, DamageRuleDefinition> rules,
            Map<NexusId, EntryDefinition> entries,
            Map<NexusId, AffixDefinition> affixes, Map<NexusId, io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorDefinition> behaviors,
            io.github.fatalitaya.combatnexus.combat.api.DomainDefinitions domains, java.util.List<ItemBindingDefinitions.Resource> itemBindings) {}
    private io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration candidate;
    public Prepared prepare(DefinitionResources resources) {
        var manager = io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.definitionDefaults(resources);
        var behaviors = io.github.fatalitaya.combatnexus.combat.internal.coordination.BehaviorDefinitionResources.read(manager);
        return new Prepared(CombatServerSettings.decode(settings(manager, "formulas"), settings(manager, "damage")),
                read(manager, "combatnexus_channels", DamageChannelRegistry.ChannelDefinition.CODEC, DamageChannelRegistry.MAX_CUSTOM_CHANNELS),
                read(manager, "combatnexus_rules", DamageRuleDefinition.CODEC, DatapackDamageRuleReloadListener.MAX_DATAPACK_RULES),
                read(manager, DatapackDamageTemplateReloadListener.ENTRY_DIRECTORY, EntryDefinition.CODEC, DatapackDamageTemplateReloadListener.MAX_ENTRY_TEMPLATES),
                read(manager, DatapackDamageTemplateReloadListener.AFFIX_DIRECTORY, AffixDefinition.CODEC, DatapackDamageTemplateReloadListener.MAX_AFFIX_TEMPLATES),
                behaviors, DomainDefinitionResources.read(manager, behaviors), ItemBindingDefinitions.read(manager));
    }
    private static com.google.gson.JsonObject settings(DefinitionResources manager, String name) {
        var id = NexusId.fromNamespaceAndPath("combatnexus", "settings/" + name + ".json");
        try (var reader = manager.required(id).openAsReader()) {
            return SettingsJson.read(reader);
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Cannot read " + id, error); }
    }
    private static <T> Map<NexusId, T> read(DefinitionResources manager, String directory, Codec<T> codec, int limit) {
        String directoryName = directory;
        var resources = manager.json(directoryName);
        if (resources.size() > limit) throw new IllegalArgumentException("Resource budget exceeded: " + directory);
        Map<NexusId, T> result = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (var reader = entry.getValue().openAsReader()) {
                var id = DefinitionResources.definitionId(directoryName, entry.getKey());
                if (id.toString().length() > 256) throw new IllegalArgumentException("Definition ID too long");
                var json = SettingsJson.read(reader);
                io.github.fatalitaya.combatnexus.combat.damage.core.definition.DamageDefinitionFields.validate(directory, json);
                T value = codec.parse(JsonOps.INSTANCE, json).getOrThrow();
                if (result.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate definition: " + id);
            } catch (java.io.IOException | RuntimeException error) {
                throw new IllegalArgumentException("Invalid combat definition: " + entry.getKey(), error);
            }
        });
        return Map.copyOf(result);
    }
    public void apply(Prepared prepared) {
        var channels = DamageChannelRegistry.buildCandidate(prepared.channels(), CombatDefinitions.nextGeneration());
        candidate = DamageChannelRegistry.withCandidate(channels, () -> {
            prepared.domains().shields().values().forEach(shield -> shield.channels().forEach(id -> {
                if (!DamageChannelRegistry.containsChannel(id)) throw new IllegalArgumentException("Unknown shield channel: " + id);
            }));
            var rules = DatapackDamageRuleReloadListener.buildCandidate(prepared.rules());
            var resolvedAffixes = new LinkedHashMap<NexusId,AffixDefinition>();
            prepared.affixes().forEach((id,affix)->resolvedAffixes.put(id,affix.resolve(key->java.util.Optional.ofNullable(prepared.entries().get(key)))));
            var templates = DatapackDamageTemplateReloadListener.buildCandidate(prepared.entries(), resolvedAffixes);
            var result = new io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration(DamageChannelRegistry.contentRevision(),
                    prepared.settings(), new io.github.fatalitaya.combatnexus.combat.damage.core.definition.DamageDefinitions(channels, rules, templates), prepared.behaviors(), prepared.domains(), io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.itemBindings(prepared.itemBindings(),defaults));
            result.itemBindings().values().forEach(result::validateBindings);
            return result;
        });
    }
    public io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration candidate() { return Objects.requireNonNull(candidate, "Definitions have not compiled"); }
}
