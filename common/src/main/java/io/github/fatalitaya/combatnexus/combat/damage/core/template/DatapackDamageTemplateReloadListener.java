package io.github.fatalitaya.combatnexus.combat.damage.core.template;

import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Atomically reloads the two typed static-template datapack directories. */
public final class DatapackDamageTemplateReloadListener {
    public static final String ENTRY_DIRECTORY =
            "combatnexus_entry_templates";
    public static final String AFFIX_DIRECTORY =
            "combatnexus_affix_templates";
    public static final int MAX_ENTRY_TEMPLATES =
            DamageTemplateLimits.MAX_ENTRY_TEMPLATES;
    public static final int MAX_AFFIX_TEMPLATES =
            DamageTemplateLimits.MAX_AFFIX_TEMPLATES;
    public static final int MAX_TEMPLATE_RULES =
            DamageTemplateLimits.MAX_TEMPLATE_RULES;
    public static final int MAX_TEMPLATE_CONDITION_NODES =
            DamageTemplateLimits.MAX_TEMPLATE_CONDITION_NODES;
    public static final int MAX_TEMPLATE_OPERATIONS =
            DamageTemplateLimits.MAX_TEMPLATE_OPERATIONS;
    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean applyPreparedForTesting(
            Map<NexusId, EntryDefinition> entries,
            Map<NexusId, AffixDefinition> affixes
    ) {
        return publishPrepared(new Prepared(entries, affixes));
    }

    public static DamageTemplateSnapshot buildCandidate(
            Map<NexusId, EntryDefinition> entries,
            Map<NexusId, AffixDefinition> affixes) {
        Validated validated = validatePrepared(new Prepared(entries, affixes));
        return new DamageTemplateSnapshot(DamageChannelRegistry.contentRevision(),
                validated.channelRevision(), true, validated.entries(), validated.affixes());
    }

    private static boolean publishPrepared(Prepared prepared) {
        try {
            Validated validated = validatePrepared(prepared);
            DamageTemplateRegistry.publishDatapack(
                    validated.entries(), validated.affixes(),
                    validated.channelRevision());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Validated validatePrepared(Prepared prepared) {
        if (prepared == null || prepared.entries() == null
                || prepared.affixes() == null) {
            throw new IllegalArgumentException("prepared_templates_are_null");
        }
        if (prepared.entries().size() > MAX_ENTRY_TEMPLATES) {
            throw new IllegalArgumentException(
                    "template_aggregate_budget_exceeded category=entry_templates"
                            + " actual=" + prepared.entries().size()
                            + " maximum=" + MAX_ENTRY_TEMPLATES
                            + " reason=aggregate_limit");
        }
        if (prepared.affixes().size() > MAX_AFFIX_TEMPLATES) {
            throw new IllegalArgumentException(
                    "template_aggregate_budget_exceeded category=affix_templates"
                            + " actual=" + prepared.affixes().size()
                            + " maximum=" + MAX_AFFIX_TEMPLATES
                            + " reason=aggregate_limit");
        }

        Map<NexusId, EntryDefinition> entries =
                sorted(prepared.entries());
        Map<NexusId, AffixDefinition> affixes =
                sorted(prepared.affixes());
        Map<NexusId, NexusId> entryDefinitionFiles = new HashMap<>();
        Map<NexusId, NexusId> affixDefinitionFiles = new HashMap<>();
        long channelRevision = DamageChannelRegistry.contentRevision();

        for (var item : entries.entrySet()) {
            NexusId fileId = Objects.requireNonNull(item.getKey(), "fileId");
            EntryDefinition definition = Objects.requireNonNull(
                    item.getValue(), "entry definition from " + fileId);
            requireFileId(fileId, definition.id(), "entry");
            requireUnique(entryDefinitionFiles, definition.id(), fileId, "entry");

            DamageTemplateDefinitionValidator.requireEntry(
                    definition, "datapack_template/entry/" + fileId, true);
        }

        for (var item : affixes.entrySet()) {
            NexusId fileId = Objects.requireNonNull(item.getKey(), "fileId");
            AffixDefinition definition = Objects.requireNonNull(
                    item.getValue(), "affix definition from " + fileId);
            requireFileId(fileId, definition.id(), "affix");
            requireUnique(affixDefinitionFiles, definition.id(), fileId, "affix");

            DamageTemplateDefinitionValidator.requireAffix(
                    definition, "datapack_template/affix/" + fileId, true);
        }

        DamageTemplateLimits.requireWithinLimits(entries, affixes);
        return new Validated(entries, affixes, channelRevision);
    }

    private static void requireFileId(
            NexusId fileId,
            NexusId definitionId,
            String kind
    ) {
        if (!fileId.equals(definitionId)) {
            throw new IllegalArgumentException(
                    kind + "_template_id_mismatch file=" + fileId
                            + " definition=" + definitionId);
        }
    }

    private static void requireUnique(
            Map<NexusId, NexusId> firstFiles,
            NexusId definitionId,
            NexusId fileId,
            String kind
    ) {
        NexusId previous = firstFiles.putIfAbsent(definitionId, fileId);
        if (previous != null) {
            throw new IllegalArgumentException(
                    "duplicate_" + kind + "_template_definition id="
                            + definitionId + " first=" + previous
                            + " duplicate=" + fileId);
        }
    }

    private static <T> Map<NexusId, T> sorted(Map<NexusId, T> input) {
        List<Map.Entry<NexusId, T>> values =
                new ArrayList<>(input.entrySet());
        values.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        LinkedHashMap<NexusId, T> result = new LinkedHashMap<>();
        for (var value : values) {
            result.put(value.getKey(), value.getValue());
        }
        return Map.copyOf(result);
    }

    public record Prepared(
            Map<NexusId, EntryDefinition> entries,
            Map<NexusId, AffixDefinition> affixes
    ) {}

    private record Validated(
            Map<NexusId, EntryDefinition> entries,
            Map<NexusId, AffixDefinition> affixes,
            long channelRevision
    ) {}
}
