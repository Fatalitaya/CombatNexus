package io.github.fatalitaya.combatnexus.combat.damage.api;

import io.github.fatalitaya.combatnexus.CombatNexusCommon;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/**
 * Central factory for identifiers owned by DamageModule itself.
 *
 * <p>External mods must preserve their own namespace by constructing their
 * complete {@link NexusId}; this helper must not be used to rewrite external IDs.</p>
 */
public final class DamageModuleIds {

    private DamageModuleIds() {
    }

    /** Creates a {@code combatnexus:*} identifier for a built-in resource only. */
    public static NexusId id(String path) {
        return NexusId.fromNamespaceAndPath(CombatNexusCommon.MOD_ID, path);
    }
}
