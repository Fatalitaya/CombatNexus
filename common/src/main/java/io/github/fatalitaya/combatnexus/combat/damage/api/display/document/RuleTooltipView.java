package io.github.fatalitaya.combatnexus.combat.damage.api.display.document;

import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleRole;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleStacking;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.narrative.RuleNarrative;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RuleTooltipView(
        NexusId id,
        RuleNarrative narrative,
        DamagePhase phase,
        DamageRuleRole role,
        int priority,
        DamageRuleStacking stacking,
        Optional<NexusId> stackingGroup,
        List<NexusId> conditionTypeIds,
        List<NexusId> operationTypeIds
) {
    public RuleTooltipView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(narrative, "narrative");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(stacking, "stacking");
        stackingGroup = stackingGroup == null ? Optional.empty() : stackingGroup;
        conditionTypeIds = conditionTypeIds == null ? List.of() : List.copyOf(conditionTypeIds);
        operationTypeIds = operationTypeIds == null ? List.of() : List.copyOf(operationTypeIds);
    }
}
