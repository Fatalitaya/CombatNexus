package io.github.fatalitaya.combatnexus.combat.internal.coordination;

import io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorSource;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision;
import java.util.*;

/** Owns protection use and cooldown policy and its installation-local consumption. */
final class ProtectionExecutor {
    private final ProtectionUseLedger uses = new ProtectionUseLedger();
    boolean available(BehaviorSource source, long now) { return source.plan().components().stream().anyMatch(c -> c instanceof io.github.fatalitaya.combatnexus.combat.api.behavior.ProtectionBehavior p && uses.available(source.installation(), c.type(), p.maximumUses(), now)); }
    Map<io.github.fatalitaya.combatnexus.api.id.NexusId, Integer> consumed(long installation) { return uses.consumed(installation); }
    Map<io.github.fatalitaya.combatnexus.api.id.NexusId, Long> remaining(long installation, long now) { return uses.remaining(installation, now); }
    void restore(long installation, Map<io.github.fatalitaya.combatnexus.api.id.NexusId, Integer> values, Map<io.github.fatalitaya.combatnexus.api.id.NexusId, Long> remaining, long now) { uses.restore(installation, values, remaining, now); }
    void retain(long installation, Set<io.github.fatalitaya.combatnexus.api.id.NexusId> types) { uses.retain(installation, types); }
    void remove(long installation) { uses.remove(installation); }
    void clear() { uses.clear(); }

    WorldBehaviorSources.GuardDecision guard(CombatRuntime world, WorldBehaviorSources sources,
            io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardContext input, List<BehaviorSource> captured) {
        if (!CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION))
            throw new IllegalStateException("Damage guard requires application");
        var lineage = CombatExecutionScope.currentLineage(world.serverSession()).orElseThrow();
        if (lineage.operation() != io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE)
            throw new IllegalStateException("Damage guard requires damage");
        world.serverRuntime().ticket(lineage).bindTarget(world, input.target());
        if (captured.size() > 64 || captured.stream().anyMatch(s -> !s.target().equals(input.target()) || !s.session().equals(world.sessionId())))
            throw new IllegalArgumentException("Foreign damage guard input");
        for (var source : captured) {
            if (!sources.valid(source)) continue;
            for (var component : source.plan().components()) {
                if (!(component instanceof io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardBehavior guard)
                        || guard.stage() != input.stage()
                        || !uses.available(source.installation(), component.type(), guard.maximumUses(), world.businessTick(source.target(), source.source()))) continue;
                Optional<io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan> plan;
                try (var compute = CombatExecutionScope.computation()) {
                    plan = Objects.requireNonNull(guard.select(new io.github.fatalitaya.combatnexus.combat.api.behavior.DamageGuardContext(
                            input.target(), input.stage(), input.requestKind(), input.sourceTags(), input.action(),
                            input.health(), input.maximumHealth(), input.damage(), input.remainingHealthDamage(), input.inputs(), source)));
                }
                if (plan.isEmpty()) continue;
                uses.consume(source.installation(), component.type(), guard.cooldownTicks(), world.businessTick(source.target(), source.source()));
                return new WorldBehaviorSources.GuardDecision(true, plan.get(), Optional.of(ApplicationJournal.Entry.write(
                        ApplicationJournal.Resource.PROTECTION, source.source(), 1, 0, 1, true)));
            }
        }
        return WorldBehaviorSources.GuardDecision.NONE;
    }

    ProtectionDecision decide(CombatRuntime world, WorldBehaviorSources sources, UUID target,
            List<BehaviorSource> captured, float before, float proposed, io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs inputs, java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.CombatSource> origin) {
        var server = world.serverRuntime();
        if (!CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION))
            throw new IllegalStateException("Protection requires admitted application");
        var lineage = CombatExecutionScope.currentLineage(world.serverSession()).orElseThrow();
        if (lineage.operation() != io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE
                && lineage.operation() != io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.LETHAL)
            throw new IllegalStateException("Protection belongs to damage application");
        server.ticket(lineage).bindTarget(world, target);
        if (captured.size() > 64 || captured.stream().anyMatch(s -> !s.target().equals(target) || !s.session().equals(world.sessionId())))
            throw new IllegalArgumentException("Foreign protection input");
        if (before <= 0 || !Float.isFinite(proposed) || proposed >= before)
            return new ProtectionDecision(proposed, Optional.empty());
        for (var source : captured) {
            if (!sources.valid(source) || !available(source, world.businessTick(source.target(), source.source()))) continue;
            for (var component : source.plan().components()) if (component instanceof io.github.fatalitaya.combatnexus.combat.api.behavior.ProtectionBehavior protection
                    && uses.available(source.installation(), component.type(), protection.maximumUses(), world.businessTick(source.target(), source.source()))) {
                float health;
                try (var compute = CombatExecutionScope.computation()) {
                    var decision = protection.protect(new io.github.fatalitaya.combatnexus.combat.api.behavior.ProtectionContext(target,before,proposed,lineage.operation(),origin,inputs,source));
                    health = decision.isPresent() ? (float)decision.getAsDouble() : proposed;
                    if (!Float.isFinite(health) || health < proposed || health > before) throw new IllegalArgumentException("Protection cannot increase health beyond pre-write health");
                }
                catch (RuntimeException failure) { continue; }
                if (health == proposed) continue;
                uses.consume(source.installation(), component.type(), protection.cooldownTicks(), world.businessTick(source.target(), source.source()));
                return new ProtectionDecision(health, Optional.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.PROTECTION, source.source(), 1, 0, 1, true)));
            }
        }
        return new ProtectionDecision(proposed, Optional.empty());
    }
}
