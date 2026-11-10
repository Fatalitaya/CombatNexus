package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput;

/**
 * Mutable per-transaction combat flags that are not part of the damage packet
 * math, source classification, event writeback, or diagnostics lifecycle.
 */
final class DamageCombatState {

    private final CriticalDecisionState criticalDecision = new CriticalDecisionState();
    private boolean attributeScalingApplied;
    private boolean armorHandled = false;
    private float armorEffectivenessMultiplier = 1.0f;
    private final Map<DefenseInput, Float> defenseInputs = new EnumMap<>(DefenseInput.class);
    private final Set<DefenseInput> consumedInputs = EnumSet.noneOf(DefenseInput.class);

    float defenseInput(DefenseInput input, float initial) {
        return defenseInputs.getOrDefault(input, initial);
    }
    boolean consumed(DefenseInput input) { return consumedInputs.contains(input); }
    void consume(DefenseInput input) { consumedInputs.add(input); }
    void setDefenseInput(DefenseInput input, float value) { defenseInputs.put(input, value); }

    Checkpoint checkpoint() {
        return new Checkpoint(
                criticalDecision.checkpoint(),
                attributeScalingApplied,
                armorHandled,
                armorEffectivenessMultiplier,
                Map.copyOf(defenseInputs), Set.copyOf(consumedInputs)
        );
    }

    void restore(Checkpoint checkpoint) {
        defenseInputs.clear(); defenseInputs.putAll(checkpoint.defenseInputs);
        consumedInputs.clear(); consumedInputs.addAll(checkpoint.consumedInputs);
        this.criticalDecision.restore(checkpoint.criticalDecision);
        this.attributeScalingApplied = checkpoint.attributeScalingApplied;
        this.armorHandled = checkpoint.armorHandled;
        this.armorEffectivenessMultiplier =
                checkpoint.armorEffectivenessMultiplier;
    }

    void markCritical() {
        throw new IllegalStateException("Critical state is owned by the decision lifecycle");
    }

    boolean critical() {
        return criticalDecision.critical();
    }

    CriticalDecisionState criticalDecision() {
        return criticalDecision;
    }

    boolean claimAttributeScaling() {
        if (attributeScalingApplied) {
            return false;
        }
        attributeScalingApplied = true;
        return true;
    }

    boolean attributeScalingApplied() {
        return attributeScalingApplied;
    }

    void markArmorHandled() {
        this.armorHandled = true;
    }

    boolean armorHandled() {
        return armorHandled;
    }

    float armorEffectivenessMultiplier() {
        return armorEffectivenessMultiplier;
    }

    void multiplyArmorEffectiveness(float multiplier) {
        float safeMultiplier = Float.isFinite(multiplier)
                ? Math.max(0.0f, multiplier)
                : 0.0f;

        armorEffectivenessMultiplier *= safeMultiplier;

        if (!Float.isFinite(armorEffectivenessMultiplier)) {
            armorEffectivenessMultiplier = 0.0f;
        }

        armorEffectivenessMultiplier =
                Math.max(0.0f, armorEffectivenessMultiplier);
    }

    record Checkpoint(
            CriticalDecisionState.Checkpoint criticalDecision,
            boolean attributeScalingApplied,
            boolean armorHandled,
            float armorEffectivenessMultiplier,
            Map<DefenseInput, Float> defenseInputs,
            Set<DefenseInput> consumedInputs
    ) {
    }
}

