package io.github.fatalitaya.combatnexus.combat.damage.api;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequest;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageResult;
import io.github.fatalitaya.combatnexus.combat.damage.core.request.DamageRequestService;
import io.github.fatalitaya.combatnexus.combat.damage.api.item.RuleItemView;

public final class DamageModuleApi {

    /** Owning server thread only; requires an adopted definition handle for the explicit server. */
    public static long definitionGeneration(io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions definitions) {
        var snapshot = definitions;
        return snapshot == null ? 0 : snapshot.generation();
    }

    public static io.github.fatalitaya.combatnexus.combat.damage.api.item.ItemCombatPresentationSnapshot itemPresentation(
            io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageItemDefinitions definitions, RuleItemView stack) {
        var snapshot = java.util.Objects.requireNonNull(
                definitions, "Server definitions required");
        return snapshot.itemPresentation(stack);
    }

    private DamageModuleApi() {
    }

    /**
     * Submits one immutable request through the authoritative server damage
     * entry and the complete DamageModule pipeline.
     *
     * <p>This method is synchronous and must be called on the owning Minecraft
     * server thread. Normal rejection and no-damage outcomes are returned as a
     * structured result. Unexpected engine or processor exceptions are not
     * swallowed. For a managed settlement, the submission scope is removed
     * before the observational {@code DamageSettledEvent} is posted. Derived
     * requests are authorized only in registered DamageModule settlement
     * callbacks, which run after that observation post. A callback submission
     * returns its result immediately; its own settlement delivery remains in
     * the coordinator FIFO until the current delivery exits. The outer FIFO is
     * drained synchronously before the root call returns.</p>
     */
    public static DamageResult submitDamage(DamageRequest request) {
        return DamageRequestService.submit(request);
    }
}
