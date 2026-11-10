package io.github.fatalitaya.combatnexus.combat.damage.core.template;

import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Map;
import java.util.Optional;

/** Immutable transaction-pinnable view of one template registry revision. */
public record DamageTemplateSnapshot(
        long revision,
        long validatedChannelRevision,
        boolean serverAuthoritative,
        Map<NexusId, EntryDefinition> entries,
        Map<NexusId, AffixDefinition> affixes
) {
    public DamageTemplateSnapshot {
        entries = entries == null ? Map.of() : Map.copyOf(entries);
        affixes = affixes == null ? Map.of() : Map.copyOf(affixes);
    }

    public Optional<EntryDefinition> entry(NexusId id) {
        return Optional.ofNullable(entries.get(id));
    }

    public Optional<AffixDefinition> affix(NexusId id) {
        return Optional.ofNullable(affixes.get(id));
    }

    /** True only for execution snapshots validated against this channel revision. */
    public boolean isCompatibleWith(long channelRevision) {
        return serverAuthoritative
                && validatedChannelRevision == channelRevision;
    }

    DamageTemplateSnapshot failClosed() {
        if (!serverAuthoritative) {
            return this;
        }
        return new DamageTemplateSnapshot(
                revision,
                validatedChannelRevision,
                false,
                entries,
                affixes
        );
    }
}
