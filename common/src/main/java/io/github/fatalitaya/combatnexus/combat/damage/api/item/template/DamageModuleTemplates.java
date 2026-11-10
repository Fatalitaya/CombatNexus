package io.github.fatalitaya.combatnexus.combat.damage.api.item.template;

import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only public lookup facade for complete static templates.
 *
 * <p>The server registry is authoritative. Clients may only observe templates
 * available from their own Java registrations; a client miss says nothing
 * about whether the server can execute the reference.</p>
 */
public final class DamageModuleTemplates {
    private DamageModuleTemplates() {}

    /** Returns the immutable definition in the currently published revision. */
    public static Optional<EntryDefinition> entry(io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions definitions, NexusId id) {
        return definitions.entry(Objects.requireNonNull(id, "id"));
    }

    /** Returns the immutable definition in the currently published revision. */
    public static Optional<AffixDefinition> affix(io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions definitions, NexusId id) {
        return definitions.affix(Objects.requireNonNull(id, "id"));
    }

    /** Monotonic runtime revision. It is not a persistent or network ID. */
    public static long revision(io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions definitions) {
        return definitions.generation();
    }

    /**
     * Reports whether the published definitions have completed strict
     * server-side validation against the current damage-channel content.
     * Lookup availability alone does not imply execution readiness.
     */
    public static boolean serverExecutionReady(io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions definitions) {
        return definitions.templatesReady();
    }

    /**
     * Channel content revision validated by the current template snapshot,
     * or {@code -1} for the Java-only pre-reload snapshot.
     */
    public static long validatedChannelRevision(io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions definitions) {
        return definitions.templateChannelRevision();
    }
}
