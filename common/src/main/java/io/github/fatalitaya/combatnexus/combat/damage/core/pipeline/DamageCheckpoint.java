package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

public record DamageCheckpoint(
        DamagePacketState.Checkpoint packet,
        DamageCombatState.Checkpoint combat,
        DamagePipelineResult.Checkpoint result,
        DamagePhaseState.Checkpoint phase,
        int contributionCount,
        int beforeStepCount
) {
}

