package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;

public interface ChannelReferencingCondition {

    List<NexusId> referencedChannels();
}
