package io.github.fatalitaya.combatnexus.combat.absorption.internal;

import io.github.fatalitaya.combatnexus.combat.api.platform.NativeAbsorptionStorage;
import io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionTarget;

/** Bound to one native entity; platform storage cannot access the underlying ledger. */
public final class NativeAbsorptionState implements NativeAbsorptionStorage.State {
    private final AbsorptionTarget owner;
    private AbsorptionLedger ledger = new AbsorptionLedger();
    private String invalid;
    public NativeAbsorptionState(AbsorptionTarget owner) { this.owner = java.util.Objects.requireNonNull(owner); }
    private void requireOwner(AbsorptionTarget target) {
        if (!owner.equals(target)) throw new IllegalArgumentException("Absorption state belongs to another entity");
        target.requireThread();
    }
    public static AbsorptionLedger ledger(AbsorptionTarget target) {
        var state = (NativeAbsorptionState)target.state();
        state.requireOwner(target);
        if (state.invalid != null) throw new IllegalStateException("Native absorption state failed restoration");
        return state.ledger;
    }
    public float maximum(AbsorptionTarget target, float original) {
        requireOwner(target);
        return (float)Math.min(Float.MAX_VALUE, original + ledger.balance());
    }
    public void reconcile(AbsorptionTarget target) { requireOwner(target); ledger.reconcile(target.amount()); }
    public String save(AbsorptionTarget target) { requireOwner(target); return invalid != null ? invalid : ledger.encode(); }
    public void restore(AbsorptionTarget target, String encoded) {
        requireOwner(target);
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireNativeWritePhase();
        ledger = new AbsorptionLedger(); invalid = null;
        try { ledger.restore(encoded); }
        catch (RuntimeException failure) {
            invalid = encoded;
        }
    }
}
