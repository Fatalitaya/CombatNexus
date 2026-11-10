package io.github.fatalitaya.combatnexus.combat.state.spi;
import java.util.Map;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.state.api.CooldownStamp;
public interface NativeCooldownTarget { Map<NexusId, CooldownStamp> combatnexus$cooldowns(); }
