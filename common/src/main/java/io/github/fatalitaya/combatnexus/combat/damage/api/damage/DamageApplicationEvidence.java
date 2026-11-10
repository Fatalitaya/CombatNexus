package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

/** Native health-write evidence is separate from whole-call observed deltas. */
public record DamageApplicationEvidence(boolean contactAccepted, boolean intercepted, boolean healthWriteObserved, float healthLoss, double shieldLoss, io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs reactionInputs, boolean applicationCompleted, io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal journal, java.util.List<io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource> behaviors, java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.action.CombatAction> action, io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result beforeApplication, java.util.List<EquipmentDamageReceipt> equipment, java.util.Optional<io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockReceipt> itemBlock) {
    public static final DamageApplicationEvidence NONE = new DamageApplicationEvidence(false, false, false, 0, 0, io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs.EMPTY, false, io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal.EMPTY, java.util.List.of(), java.util.Optional.empty(), io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Result.EMPTY, java.util.List.of(), java.util.Optional.empty());
    public DamageApplicationEvidence {
        equipment = java.util.List.copyOf(equipment);
        java.util.Objects.requireNonNull(itemBlock);
        behaviors = java.util.List.copyOf(behaviors);
        if (!Float.isFinite(healthLoss) || healthLoss < 0 || !Double.isFinite(shieldLoss) || shieldLoss < 0) {
            throw new IllegalArgumentException("Invalid application evidence");
        }
    }
}
