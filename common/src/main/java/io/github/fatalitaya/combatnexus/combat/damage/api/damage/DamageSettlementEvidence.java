package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

import java.util.*;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatAction;

/** Retainable application evidence. Executable plans and extension objects belong to delivery only. */
public record DamageSettlementEvidence(boolean healthWriteObserved, float healthLoss, double shieldLoss,
        boolean applicationCompleted, ApplicationJournal journal, Optional<CombatAction> action,
        List<EquipmentDamageReceipt> equipment, Map<UUID, AttributeSnapshot> attributes, BeforeApplication beforeApplication, Optional<io.github.fatalitaya.combatnexus.combat.blocking.api.ItemBlockReceipt> itemBlock) {
    public record BeforeStep(int index, io.github.fatalitaya.combatnexus.api.id.NexusId commandType,
            boolean applied, ApplicationJournal journal,
            Optional<io.github.fatalitaya.combatnexus.combat.api.CommandContext> context,
            Optional<io.github.fatalitaya.combatnexus.combat.api.CommandRejection> rejection) {}
    public record BeforeApplication(List<BeforeStep> steps, boolean continueParent, boolean completed) {
        public BeforeApplication { steps = List.copyOf(steps); }
    }
    public DamageSettlementEvidence { equipment = List.copyOf(equipment); attributes = Map.copyOf(attributes); Objects.requireNonNull(itemBlock); }
    public static DamageSettlementEvidence capture(DamageApplicationEvidence evidence) {
        var attributes = new LinkedHashMap<UUID, AttributeSnapshot>();
        evidence.reactionInputs().participants().forEach((id, input) -> attributes.put(id, input.attributeSnapshot()));
        return new DamageSettlementEvidence(evidence.healthWriteObserved(), evidence.healthLoss(), evidence.shieldLoss(),
            evidence.applicationCompleted(), evidence.journal(), evidence.action(), evidence.equipment(), attributes,
            new BeforeApplication(evidence.beforeApplication().steps().stream().map(step -> new BeforeStep(
                step.index(), step.commandType(), step.result().applied(), step.journal(), step.context(), step.rejection())).toList(),
                evidence.beforeApplication().continueParent(), evidence.beforeApplication().completed()), evidence.itemBlock());
    }
}
