package io.github.fatalitaya.combatnexus.combat.time.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.StateCommands;
import io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime;
import java.util.Objects;
import java.util.UUID;

/** World-owned domain commands; mutations enter through StateCommands. */
public final class WorldBusinessTimeView implements BusinessTime {
    private final CombatRuntime world;
    public WorldBusinessTimeView(CombatRuntime world) { this.world = Objects.requireNonNull(world); }

    public long now(UUID target,NexusId source) { return world.businessTick(target,source); }
    public long phase(UUID target,NexusId source,long nativePhase) { return world.timeControl().phase(target,source,nativePhase); }

}
