package io.github.fatalitaya.combatnexus.combat.api.behavior;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingSettlement;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageResult;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** Short-lived periodic command capability; implementations validate source liveness on every call. */
public interface PeriodicExecution {
    io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult ability(NexusId contribution, io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit limit);
    void environmentDamage(io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage parameters);
    void effectCapability(NexusId capability);
    BehaviorSource source();
    io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame input();
    io.github.fatalitaya.combatnexus.combat.api.CommandOutcome<?> execute(io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent intent);
    HealingSettlement heal(double amount);
    DamageResult damage(double amount, NexusId damageType);
    DamageResult damage(double amount, NexusId damageType, float minimumHealth);
}
