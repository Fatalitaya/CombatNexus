package io.github.fatalitaya.combatnexus.combat.api.action;

import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.geometry.Position3;
import io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame;
import io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch;

/** Native lifecycle observations. These records never grant mutation or retain entities. */
public sealed interface CombatMoment {
    record Attack(CombatAction action, CombatInputFrame actor, UUID directTarget, double strength) implements CombatMoment {}
    record Launched(ProjectileLaunch launch) implements CombatMoment {}
    record Collision(ProjectileLaunch launch, Position3 position, Optional<UUID> directEntity, Optional<UUID> subject) implements CombatMoment {}
    record PartHit(UUID part, UUID subject, NexusId damageType, boolean accepted) implements CombatMoment {}
    record ItemBlock(UUID target, Optional<UUID> attacker, NexusId item, double blocked) implements CombatMoment {}
    record Hit(long operation, UUID target, double healthLost, double absorptionLost, double shieldLost, Optional<CombatAction> action,
               List<io.github.fatalitaya.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt> equipment) implements CombatMoment {
        public Hit { equipment = List.copyOf(equipment); }
    }
    record PostAttack(CombatAction action, UUID directTarget) implements CombatMoment {}
    record Death(UUID target, Optional<UUID> killer, NexusId damageType, Optional<UUID> directEntity, OptionalLong operation) implements CombatMoment {}
    record EffectState(int duration, int amplifier) {}
    enum EffectChange { ADDED, REFRESHED, UPGRADED, CHANGED, REMOVED }
    record Effect(UUID target, NexusId effect, Optional<EffectState> before, Optional<EffectState> after,
                  EffectChange change, Optional<UUID> source,
                  Optional<io.github.fatalitaya.combatnexus.effect.api.EffectService.Change> managedChange) implements CombatMoment {
        public Effect {
            Objects.requireNonNull(target); Objects.requireNonNull(effect); Objects.requireNonNull(before);
            Objects.requireNonNull(after); Objects.requireNonNull(change); Objects.requireNonNull(source);
            Objects.requireNonNull(managedChange);
        }
    }
    record Protection(UUID target, OptionalLong operation, NexusId damageType, boolean protectedTarget,
                      io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal journal) implements CombatMoment {}
}
