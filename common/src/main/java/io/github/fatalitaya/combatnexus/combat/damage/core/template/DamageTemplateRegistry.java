package io.github.fatalitaya.combatnexus.combat.damage.core.template;

import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle.DamageModuleRegistrationAccess;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.core.security.DamageModuleItemSecurity;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Optional;

/** Atomically replaceable templates compiled from the shared definition resource overlay. */
public final class DamageTemplateRegistry {
    private static volatile DamageTemplateSnapshot snapshot =
            unvalidatedSnapshot(0L, Map.of(), Map.of());
    private static boolean frozen;

    private DamageTemplateRegistry() {}

    public static synchronized void freeze(
            DamageModuleRegistrationAccess access
    ) {
        DamageModuleLifecycle.requireRegistering(access, "freezeTemplates");
        if (frozen) {
            throw new IllegalStateException("Template registry already frozen");
        }
        snapshot = unvalidatedSnapshot(snapshot.revision(), Map.of(), Map.of());
        frozen = true;
    }

    public static Optional<EntryDefinition> entry(NexusId id) {
        return snapshot().entry(id);
    }

    public static Optional<AffixDefinition> affix(NexusId id) {
        return snapshot().affix(id);
    }

    public static long revision() {
        return snapshot().revision();
    }

    public static DamageTemplateSnapshot snapshot() {
        return snapshot;
    }

    /**
     * Returns a transaction-pinnable view. New callers fail closed when the
     * current channel content revision differs from the validated revision.
     */
    public static DamageTemplateSnapshot executionSnapshot() {
        DamageTemplateSnapshot current = snapshot();
        return current.isCompatibleWith(DamageChannelRegistry.contentRevision())
                ? current
                : current.failClosed();
    }

    public static boolean serverExecutionReady() {
        return snapshot().isCompatibleWith(
                DamageChannelRegistry.contentRevision());
    }

    public static long validatedChannelRevision() {
        return snapshot().validatedChannelRevision();
    }

    static synchronized void publishDatapack(
            Map<NexusId, EntryDefinition> datapackEntries,
            Map<NexusId, AffixDefinition> datapackAffixes,
            long validatedChannelRevision
    ) {
        if (!frozen) {
            throw new IllegalStateException("Template registry is not frozen");
        }
        DamageTemplateLimits.requireWithinLimits(datapackEntries, datapackAffixes);
        if (validatedChannelRevision
                != DamageChannelRegistry.contentRevision()) {
            throw new IllegalStateException(
                    "template_channel_revision_changed expected="
                            + validatedChannelRevision + " actual="
                            + DamageChannelRegistry.contentRevision());
        }
        snapshot = new DamageTemplateSnapshot(
                Math.addExact(snapshot.revision(), 1L),
                validatedChannelRevision,
                true,
                datapackEntries,
                datapackAffixes
        );
    }

    public static synchronized void resetServerState() {
        if (frozen) {
            snapshot = unvalidatedSnapshot(
                    Math.addExact(snapshot.revision(), 1L),
                    Map.of(),
                    Map.of()
            );
        }
    }

    @ApiStatus.Internal
    public static synchronized void resetForTesting() {
        snapshot = unvalidatedSnapshot(0L, Map.of(), Map.of());
        frozen = false;
    }

    private static DamageTemplateSnapshot unvalidatedSnapshot(
            long revision,
            Map<NexusId, EntryDefinition> entries,
            Map<NexusId, AffixDefinition> affixes
    ) {
        return new DamageTemplateSnapshot(
                revision,
                -1L,
                false,
                entries,
                affixes
        );
    }
}
