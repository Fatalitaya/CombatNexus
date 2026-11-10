package io.github.fatalitaya.combatnexus.combat.action.internal;

import io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.platform.EntityMotionAccess;

import java.util.*;

public final class EntityApplication {
    private EntityApplication() {}

    public static CommandResult<Boolean> apply(EntityMotionAccess access, EntityCommand command) {
        var target = command.target();
        var writes = new ArrayList<ApplicationJournal.Entry>();
        var resource =
                command instanceof EntityCommand.Ignite
                        ? ApplicationJournal.Resource.FIRE
                        : ((EntityCommand.Displace) command).mode()
                                        == EntityCommand.Displace.Mode.POSITION
                                ? ApplicationJournal.Resource.POSITION
                                : ApplicationJournal.Resource.VELOCITY;
        try {
            {
                java.util.Objects.requireNonNull(command);
                if (command instanceof EntityCommand.Ignite fire) {
                    int before = access.fireTicks(target);
                    access.fireTicks(target, Math.max(before, fire.ticks()));
                    writes.add(
                            ApplicationJournal.Entry.write(
                                    ApplicationJournal.Resource.FIRE,
                                    command.source().id(),
                                    before,
                                    access.fireTicks(target),
                                    Math.max(0, access.fireTicks(target) - before),
                                    true));
                } else if (command instanceof EntityCommand.Displace movement) {
                    var before = access.vector(target, movement.mode());
                    var after = before.add(movement.delta());
                    if (!after.finite())
                        return CommandResult.rejected(
                                CommandRejection.Code.INVALID_INPUT, "Displacement overflow");
                    access.vector(target, movement.mode(), after);
                    var actual = access.vector(target, movement.mode());
                    writes.add(
                            ApplicationJournal.Entry.write(
                                    resource,
                                    command.source().id(),
                                    before.x(),
                                    actual.x(),
                                    Math.abs(actual.x() - before.x()),
                                    true));
                    writes.add(
                            ApplicationJournal.Entry.write(
                                    resource,
                                    command.source().id(),
                                    before.y(),
                                    actual.y(),
                                    Math.abs(actual.y() - before.y()),
                                    true));
                    writes.add(
                            ApplicationJournal.Entry.write(
                                    resource,
                                    command.source().id(),
                                    before.z(),
                                    actual.z(),
                                    Math.abs(actual.z() - before.z()),
                                    true));
                } else {
                    throw new IllegalStateException("Unexpected sealed subtype");
                }
            }
            return CommandResult.applied(true).withJournal(new ApplicationJournal(writes, true));
        } catch (RuntimeException failure) {
            writes.add(
                    new ApplicationJournal.Entry(
                            resource,
                            Optional.of(command.source().id()),
                            OptionalDouble.empty(),
                            OptionalDouble.empty(),
                            0,
                            ApplicationJournal.Observation.UNKNOWN,
                            false));
            return CommandResult.<Boolean>rejected(
                            CommandRejection.Code.INCOMPLETE_APPLICATION,
                            "Entity write incomplete; no retry")
                    .withJournal(new ApplicationJournal(writes, false));
        }
    }
}
