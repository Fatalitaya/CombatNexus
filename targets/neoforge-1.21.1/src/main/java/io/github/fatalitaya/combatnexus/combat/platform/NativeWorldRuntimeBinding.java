package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.combat.api.transaction.WorldSession;
import io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding;

public interface NativeWorldRuntimeBinding extends WorldRuntimeBinding {
    void combatnexus$bindWorldSession(WorldSession session);
}
