package io.github.fatalitaya.combatnexus.combat.healing.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingContribution;
import io.github.fatalitaya.combatnexus.formula.Amounts;
import java.util.*;

/** Numerical amount and explanation owned together, independent of callback lifetime. */
public final class HealingNumericalState {
    private double amount;
    private final List<HealingContribution> contributions = new ArrayList<>();
    public HealingNumericalState(double amount) { this.amount = amount; }
    public double amount() { return amount; }
    public List<HealingContribution> contributions() { return List.copyOf(contributions); }
    public void amount(NexusId source, HealingContribution.Kind kind, double value) {
        Amounts.nonNegative(value, "healing contribution");
        record(source, kind, value);
    }
    public void additive(NexusId source, double delta) {
        if (!Double.isFinite(delta)) throw new IllegalArgumentException("Nonfinite healing additive");
        record(source, HealingContribution.Kind.ADDITIVE, amount + delta);
    }
    private void record(NexusId source, HealingContribution.Kind kind, double value) {
        if (contributions.size() >= HealingContribution.MAX_FACTS) throw new IllegalStateException("Healing contribution capacity exceeded");
        contributions.add(new HealingContribution(source, kind, amount, value)); amount = value;
    }
    public record Checkpoint(double amount, List<HealingContribution> contributions) {
        public Checkpoint { contributions = List.copyOf(contributions); }
    }
    public Checkpoint checkpoint() { return new Checkpoint(amount, contributions); }
    public void restore(Checkpoint checkpoint) {
        amount = checkpoint.amount(); contributions.clear(); contributions.addAll(checkpoint.contributions());
    }
    /** The caller supplies matched contributions in their established execution order. */
    public void reduce(List<io.github.fatalitaya.combatnexus.combat.healing.api.HealingRuleContribution> matching) {
        for (var entry : matching) if (entry.additive() != 0) additive(entry.source(), entry.additive());
        amount(NexusId.fromNamespaceAndPath("combatnexus", "healing_nonnegative"), HealingContribution.Kind.CLAMP, Math.max(0, amount));
        for (var entry : matching) if (entry.multiplier() != 1) amount(entry.source(), HealingContribution.Kind.ADJUSTMENT, amount * entry.multiplier());
    }
}
