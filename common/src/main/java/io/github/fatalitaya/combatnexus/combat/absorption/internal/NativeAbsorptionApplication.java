package io.github.fatalitaya.combatnexus.combat.absorption.internal;

import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.platform.AbsorptionTarget;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.absorption.api.*;

public final class NativeAbsorptionApplication {
    private NativeAbsorptionApplication() {}
    public static CommandResult<AbsorptionSource> apply(AbsorptionTarget target, AbsorptionCommand command) {
        var ledger = NativeAbsorptionState.ledger(target);
        float before = target.amount();
        double owned = ledger.balance();
        boolean completed = false;
        try {
            ledger.change(command, before);
            target.amount((float)Math.max(0, Math.min(Float.MAX_VALUE, before + ledger.balance() - owned)));
            completed = true;
        } catch (RuntimeException failure) {
        } finally {
            ledger.reconcile(target.amount());
        }
        var journal = new ApplicationJournal(List.of(new ApplicationJournal.Entry(ApplicationJournal.Resource.ABSORPTION,
                Optional.of(command.source().id()), OptionalDouble.of(before), OptionalDouble.of(target.amount()),
                completed ? Math.abs(target.amount() - before) : 0,
                completed ? ApplicationJournal.Observation.NATIVE_WRITE : ApplicationJournal.Observation.UNKNOWN, completed)), completed);
        if (!completed) return CommandResult.<AbsorptionSource>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Absorption application incomplete; do not retry").withJournal(journal);
        var source = ledger.snapshot().stream().filter(s -> s.source().id().equals(command.source().id()) && s.source().owner().equals(command.source().owner())).findFirst().orElseThrow();
        return CommandResult.applied(source).withJournal(journal);
    }
}
