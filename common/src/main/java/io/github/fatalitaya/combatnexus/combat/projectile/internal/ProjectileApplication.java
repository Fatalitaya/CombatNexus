package io.github.fatalitaya.combatnexus.combat.projectile.internal;

import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.projectile.api.*;

public final class ProjectileApplication {
    private ProjectileApplication() {}
    public static CommandResult<Double> modify(ProjectileDamageWorldAccess world, ProjectileChange command) {
        var projectile = command.projectile();
        if (!world.available(projectile)) return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Arrow launch identity/timing mismatch");
        var launch = projectile.launch();
        if (launch.isEmpty() || !launch.get().action().equals(command.action())
                || !launch.get().shooter().equals(command.target())
                || launch.get().gameTick() != world.gameTick())
            return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Arrow launch identity/timing mismatch");
        double before = projectile.damage(), after = before * command.damageMultiplier();
        if (!Double.isFinite(after)) return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Projectile damage overflow");
        try {
            projectile.damage(after);
            return CommandResult.applied(projectile.damage()).withJournal(new ApplicationJournal(List.of(
                    ApplicationJournal.Entry.write(ApplicationJournal.Resource.PROJECTILE, command.source().id(), before, projectile.damage(), Math.abs(projectile.damage()-before), true)), true));
        } catch (RuntimeException failure) {
            return CommandResult.<Double>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Projectile modification incomplete; do not retry")
                    .withJournal(new ApplicationJournal(List.of(new ApplicationJournal.Entry(ApplicationJournal.Resource.PROJECTILE,
                            Optional.of(command.source().id()), OptionalDouble.of(before), OptionalDouble.of(projectile.damage()), 0,
                            ApplicationJournal.Observation.UNKNOWN, false)), false));
        }
    }
}
