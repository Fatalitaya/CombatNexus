package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.combat.api.platform.ParticipantStateAccess;
import io.github.fatalitaya.combatnexus.combat.state.api.StateKey;
import io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates;
import io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

public record WorldParticipantStates(ServerLevel level) implements ParticipantStateAccess {
    public WorldParticipantStates { Objects.requireNonNull(level); }
    public void actionFinished(UUID participant, UUID action) {
        if (level.getEntity(participant) instanceof NativeRuntimeStates states)
            states.combatnexus$states().clear(StateKey.Boundary.ACTION_FINISHED, Optional.of(action));
    }
    public void sourceRemoved(UUID participant, UUID source) {
        var entity = level.getEntity(participant);
        if (entity instanceof NativeSchedules schedules)
            schedules.combatnexus$schedules().clear(StateKey.Boundary.SOURCE_REMOVED, Optional.of(source));
        if (entity instanceof NativeRuntimeStates states)
            states.combatnexus$states().clear(StateKey.Boundary.SOURCE_REMOVED, Optional.of(source));
    }
}
