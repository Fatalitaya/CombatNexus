package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;

/**
 * Implemented by built-in operations that reference serialized DamageModule
 * channel ids.
 * <p>
 * These references are validated during datapack rule reload so typoed channel
 * ids do not silently degrade to untyped at runtime.
 */
public interface ChannelReferencingOperation {

    List<NexusId> referencedChannels();
}
