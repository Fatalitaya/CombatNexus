package io.github.fatalitaya.combatnexus.combat.damage.core.contribution;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageContributionDescriptor;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageContributionStatus;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DamageContributionCollector {

    private final List<io.github.fatalitaya.combatnexus.combat.damage.api.display.ContributionRecord> entries = new ArrayList<>();
    public void recordFact(DamageMutationResult result, io.github.fatalitaya.combatnexus.combat.damage.api.display.ContributionRecord fact) {
        entries.add(java.util.Objects.requireNonNull(fact).withStatus(statusFromResult(java.util.Objects.requireNonNull(result))));
    }
    public List<io.github.fatalitaya.combatnexus.combat.damage.api.display.ContributionRecord> facts() { return List.copyOf(entries); }
    public List<DamageContributionDescriptor> entries() {
        return entries.stream().map(io.github.fatalitaya.combatnexus.combat.damage.api.display.ContributionProjector::project).toList();
    }

    public List<DamageContributionDescriptor> applied() {
        return filtered(DamageContributionStatus.APPLIED);
    }

    public List<DamageContributionDescriptor> rejected() {
        return filtered(DamageContributionStatus.REJECTED);
    }

    public List<DamageContributionDescriptor> noOps() {
        return filtered(DamageContributionStatus.NO_OP);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @ApiStatus.Internal
    public int checkpoint() {
        return entries.size();
    }

    @ApiStatus.Internal
    public void rollback(int checkpoint) {
        if (checkpoint < 0 || checkpoint > entries.size()) {
            throw new IllegalArgumentException(
                    "Invalid contribution checkpoint: " + checkpoint
            );
        }
        entries.subList(checkpoint, entries.size()).clear();
    }

    private List<DamageContributionDescriptor> filtered(
            DamageContributionStatus status
    ) {
        return entries.stream()
                .filter(entry -> entry.status() == status)
                .map(io.github.fatalitaya.combatnexus.combat.damage.api.display.ContributionProjector::project)
                .toList();
    }

    private static DamageContributionStatus statusFromResult(
            DamageMutationResult result
    ) {
        if (result.applied()) {
            return DamageContributionStatus.APPLIED;
        }

        if (result.rejected()) {
            return DamageContributionStatus.REJECTED;
        }

        return DamageContributionStatus.NO_OP;
    }
}
