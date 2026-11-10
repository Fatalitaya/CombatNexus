package io.github.fatalitaya.combatnexus.combat.health.internal;

import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.platform.HealthTarget;
import io.github.fatalitaya.combatnexus.combat.api.platform.LethalTarget;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.health.api.*;
import io.github.fatalitaya.combatnexus.combat.spi.NativeResourceWrites;
import io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision;

/** Vanilla application only; policy and transaction admission are owned by the coordinator. */
public final class NativeHealthApplication {
    private NativeHealthApplication() {}
    public static CommandResult<Float> correct(HealthTarget target, HealthCorrection command) {
        float before = target.health();
        if (command.health() <= 0 || command.reason() == HealthCorrection.Reason.LIFE_COST && command.health() > before)
            return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Correction must be nonlethal; a life cost cannot increase health");
        var writes = new ArrayList<ApplicationJournal.Entry>();
        boolean complete = write(target, command.source(), before, (float)Math.min(target.maximumHealth(), command.health()), writes);
        return (complete ? CommandResult.applied(target.health()) : CommandResult.<Float>rejected(
                CommandRejection.Code.INCOMPLETE_APPLICATION, "Health correction incomplete; do not retry"))
                .withJournal(new ApplicationJournal(writes, complete));
    }
    public static CommandResult<LethalReceipt> execute(LethalTarget target, LethalAttempt command, ProtectionDecision managed) {
        float before = target.health();
        var writes = new ArrayList<ApplicationJournal.Entry>();
        managed.consumption().ifPresent(writes::add);
        try {
            if (managed.consumption().isPresent()) {
                if (!write(target, command.source(), before, managed.health(), writes)) return incomplete(writes);
                return result(target, before, LethalReceipt.Outcome.MANAGED_PROTECTED, writes);
            }
            // The native protection boundary owns item consumption, effects and its own health write.
            boolean protectedByNative;
            try { protectedByNative = target.protect(); }
            finally { writes.addAll(target.protectionEvidence()); }
            if (protectedByNative) {
                writes.add(new ApplicationJournal.Entry(ApplicationJournal.Resource.HEALTH, Optional.empty(),
                        OptionalDouble.of(before), OptionalDouble.of(target.health()), 0, ApplicationJournal.Observation.WHOLE_CALL, true));
                return result(target, before, LethalReceipt.Outcome.NATIVE_PROTECTED, writes);
            }
            if (target.removed() || !target.alive()) return result(target, before, LethalReceipt.Outcome.DEATH_REFUSED, writes);
            target.deathSound();
            target.rememberAttacker();
            target.recordDamage(before);
            if (!write(target, command.source(), before, 0, writes)) return incomplete(writes);
            target.die();
            return result(target, before, target.deathConfirmed()
                    ? LethalReceipt.Outcome.DEATH_CONFIRMED : LethalReceipt.Outcome.DEATH_REFUSED, writes);
        } catch (RuntimeException failure) {
            return incomplete(writes);
        }
    }
    private static CommandResult<LethalReceipt> incomplete(List<ApplicationJournal.Entry> writes) {
        return CommandResult.<LethalReceipt>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Lethal application incomplete; do not retry")
                .withJournal(new ApplicationJournal(writes, false));
    }
    private static CommandResult<LethalReceipt> result(HealthTarget target, float before, LethalReceipt.Outcome outcome, List<ApplicationJournal.Entry> writes) {
        return CommandResult.applied(new LethalReceipt(outcome, before, target.health())).withJournal(new ApplicationJournal(writes, true));
    }
    private static boolean write(HealthTarget target, CombatSource source, float before, float next, List<ApplicationJournal.Entry> writes) {
        int index = writes.size();
        writes.add(new ApplicationJournal.Entry(ApplicationJournal.Resource.HEALTH, Optional.of(source.id()), OptionalDouble.of(before),
                OptionalDouble.empty(), 0, ApplicationJournal.Observation.UNKNOWN, false));
        try (var token = NativeResourceWrites.health(target)) {
            boolean complete = false;
            try { target.health(next); complete = true; }
            catch (RuntimeException failure) {}
            if (token.uncontended()) writes.set(index, ApplicationJournal.Entry.write(ApplicationJournal.Resource.HEALTH,
                    source.id(), before, target.health(), Math.abs(before - target.health()), complete));
            return complete && token.uncontended();
        }
    }
}
